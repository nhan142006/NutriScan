package vn.edu.vlu.milestone2.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import vn.edu.vlu.milestone2.data.repository.ScanRepository
import vn.edu.vlu.milestone2.di.ServiceLocator

/**
 * Hàng đợi offline: chạy khi có mạng (Constraint CONNECTED), đọc ảnh từ file URI,
 * gửi Gemini với structured output rồi cập nhật ScanResult (PENDING → DONE / FAILED).
 */
class LabelAnalysisWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val scanId = inputData.getLong(KEY_SCAN_ID, -1L)
        if (scanId <= 0) return Result.failure()
        ServiceLocator.init(applicationContext)
        val outcome = ServiceLocator.scanRepository.processPendingScan(
            scanId = scanId,
            isLastAttempt = runAttemptCount >= MAX_ATTEMPTS
        )
        return when (outcome) {
            ScanRepository.WorkOutcome.SUCCESS -> Result.success()
            ScanRepository.WorkOutcome.RETRY -> Result.retry()
            ScanRepository.WorkOutcome.FAILED -> Result.failure()
        }
    }

    companion object {
        const val KEY_SCAN_ID = "scan_id"
        const val MAX_ATTEMPTS = 5
    }
}
