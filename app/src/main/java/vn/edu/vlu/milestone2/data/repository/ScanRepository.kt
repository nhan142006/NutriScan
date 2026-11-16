package vn.edu.vlu.milestone2.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.data.local.dao.ScanResultDao
import vn.edu.vlu.milestone2.data.local.entity.ScanResultEntity
import vn.edu.vlu.milestone2.data.local.entity.ScanSource
import vn.edu.vlu.milestone2.data.local.entity.ScanStatus
import vn.edu.vlu.milestone2.data.model.LabelAnalysis
import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.data.model.ScanResult
import vn.edu.vlu.milestone2.data.remote.gemini.GeminiDataSource
import vn.edu.vlu.milestone2.data.storage.ScanImageStorage
import vn.edu.vlu.milestone2.domain.NutritionAnalyzer
import vn.edu.vlu.milestone2.work.ScanWorkScheduler
import java.io.File

/**
 * Module "AI Label Scan & Analysis" + Lịch sử quét (CRUD).
 * Ảnh: chỉ lưu file URI → [ScanImageStorage]; phân tích ảnh chạy trong WorkManager khi có mạng.
 */
class ScanRepository(
    private val scanDao: ScanResultDao,
    private val gemini: GeminiDataSource,
    private val analyzer: NutritionAnalyzer,
    private val imageStorage: ScanImageStorage,
    private val scheduler: ScanWorkScheduler,
    private val profileRepository: HealthProfileRepository,
    private val gson: Gson
) {

    enum class WorkOutcome { SUCCESS, RETRY, FAILED }

    /** Kết quả phân tích sản phẩm theo mã vạch. [usedAi] = false khi phải dùng luật offline. */
    data class ProductAnalysis(val scanId: Long, val usedAi: Boolean, val aiError: NetworkError?)

    fun observeHistory(): Flow<List<ScanResult>> = scanDao.observeAll().map { it.map(::mapScan) }

    fun observeRecent(limit: Int): Flow<List<ScanResult>> = scanDao.observeRecent(limit).map { it.map(::mapScan) }

    fun observeScan(scanId: Long): Flow<ScanResult?> = scanDao.observe(scanId).map { it?.let(::mapScan) }

    fun observePendingCount(): Flow<Int> = scanDao.observeCount(ScanStatus.PENDING)

    suspend fun getScan(scanId: Long): ScanResult? = scanDao.get(scanId)?.let(::mapScan)

    suspend fun getRecentUsable(limit: Int): List<ScanResult> = scanDao.getRecentUsable(limit).map(::mapScan)

    suspend fun getRecentAvoided(limit: Int): List<ScanResult> = scanDao.getRecentAvoided(limit).map(::mapScan)

    /**
     * Barcode → Open Food Facts → Gemini (structured JSON). Nếu chưa cấu hình key / mất mạng
     * thì dùng [NutritionAnalyzer] offline để người dùng vẫn có kết quả ngay trong siêu thị.
     */
    suspend fun analyzeProduct(product: Product): ProductAnalysis {
        val profile = profileRepository.getCurrentProfile()
        val aiResult = gemini.analyzeProduct(product, profile)
        val (analysis, aiError) = when (aiResult) {
            is NetworkResult.Success -> aiResult.data to null
            is NetworkResult.Error -> analyzer.analyze(product, profile) to aiResult.error
        }
        val scanId = scanDao.insert(
            ScanResultEntity(
                productBarcode = product.barcode,
                imagePath = null,
                productName = product.name,
                ingredientsJson = gson.toJsonList(analysis.ingredients),
                rating = analysis.rating,
                analysisJson = gson.toJson(analysis),
                note = null,
                status = ScanStatus.DONE,
                source = ScanSource.BARCODE,
                errorMessage = null,
                createdAt = System.currentTimeMillis()
            )
        )
        return ProductAnalysis(scanId, usedAi = aiError == null, aiError = aiError)
    }

    /**
     * Ảnh nhãn: tạo bản ghi PENDING → nén ảnh vào filesDir/scans/{scanId}.jpg → lưu FILE URI
     * → xếp hàng WorkManager (chạy ngay nếu online, tự chạy lại khi có mạng nếu offline).
     */
    suspend fun createLabelScan(captureFile: File): Result<Long> {
        val scanId = scanDao.insert(
            ScanResultEntity(
                productBarcode = null,
                imagePath = null,
                productName = PENDING_NAME,
                ingredientsJson = "[]",
                rating = null,
                analysisJson = null,
                note = null,
                status = ScanStatus.PENDING,
                source = ScanSource.LABEL_PHOTO,
                errorMessage = null,
                createdAt = System.currentTimeMillis()
            )
        )
        return try {
            val file = imageStorage.compressToScanFile(captureFile, scanId)
            scanDao.updateImagePath(scanId, imageStorage.toFileUri(file))
            scheduler.enqueue(scanId)
            Result.success(scanId)
        } catch (error: Exception) {
            scanDao.delete(scanId)
            Result.failure(error)
        }
    }

    /** Được gọi bởi LabelAnalysisWorker. */
    suspend fun processPendingScan(scanId: Long, isLastAttempt: Boolean): WorkOutcome {
        val scan = scanDao.get(scanId) ?: return WorkOutcome.FAILED
        if (scan.status == ScanStatus.DONE) return WorkOutcome.SUCCESS
        val bytes = imageStorage.readBytes(scan.imagePath)
        if (bytes == null) {
            scanDao.updateStatus(scanId, ScanStatus.FAILED, "Không tìm thấy ảnh đã lưu")
            return WorkOutcome.FAILED
        }
        val profile = profileRepository.getCurrentProfile()
        return when (val result = gemini.analyzeLabelImage(bytes, profile)) {
            is NetworkResult.Success -> {
                saveAnalysis(scan, result.data)
                WorkOutcome.SUCCESS
            }
            is NetworkResult.Error -> {
                val retryable = result.error in RETRYABLE_ERRORS
                if (retryable && !isLastAttempt) {
                    WorkOutcome.RETRY // giữ trạng thái PENDING
                } else {
                    scanDao.updateStatus(scanId, ScanStatus.FAILED, result.error.name)
                    WorkOutcome.FAILED
                }
            }
        }
    }

    suspend fun retryScan(scanId: Long) {
        scanDao.updateStatus(scanId, ScanStatus.PENDING, null)
        scheduler.enqueue(scanId)
    }

    suspend fun updateNote(scanId: Long, note: String?) {
        scanDao.updateNote(scanId, note?.trim()?.takeIf { it.isNotEmpty() })
    }

    /** Xóa bản ghi + xóa file ảnh tương ứng (Proposal 4.4 – Cleanup). */
    suspend fun deleteScan(scanId: Long) {
        val scan = scanDao.get(scanId) ?: return
        scheduler.cancel(scanId)
        imageStorage.delete(scan.imagePath)
        scanDao.delete(scanId)
    }

    private suspend fun saveAnalysis(scan: ScanResultEntity, analysis: LabelAnalysis) {
        scanDao.update(
            scan.copy(
                productName = analysis.productName,
                ingredientsJson = gson.toJsonList(analysis.ingredients),
                rating = analysis.rating,
                analysisJson = gson.toJson(analysis),
                status = ScanStatus.DONE,
                errorMessage = null
            )
        )
    }

    private fun mapScan(entity: ScanResultEntity): ScanResult = entity.toDomain(gson, gemini::parseLabelAnalysis)

    private companion object {
        const val PENDING_NAME = "Ảnh nhãn đang chờ phân tích"
        val RETRYABLE_ERRORS = setOf(
            NetworkError.NO_INTERNET, NetworkError.TIMEOUT, NetworkError.SERVER, NetworkError.TOO_MANY_REQUESTS
        )
    }
}
