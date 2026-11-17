package vn.edu.vlu.milestone2.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Chính sách lưu ảnh (Proposal 4.4 – ràng buộc #2 của giảng viên):
 *  1. CameraX ghi ảnh gốc vào file tạm trong cacheDir.
 *  2. Thu nhỏ (cạnh dài ≤ 1280 px) + nén JPEG (quality 80 → giảm dần) cho tới khi < 1 MB.
 *  3. Lưu vào filesDir/scans/{scanId}.jpg; Room chỉ lưu FILE URI. Không Base64/ByteArray trong DB.
 *  4. Xóa file tạm; xóa ScanResult thì xóa luôn file ảnh.
 */
class ScanImageStorage(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val appContext = context.applicationContext
    private val scansDir: File get() = File(appContext.filesDir, SCANS_DIR).apply { mkdirs() }

    /** File tạm cho CameraX ImageCapture.OutputFileOptions. */
    fun createTempCaptureFile(): File = File.createTempFile("capture_", ".jpg", appContext.cacheDir)

    suspend fun compressToScanFile(source: File, scanId: Long): File = withContext(ioDispatcher) {
        try {
            val bitmap = decodeScaled(source) ?: error("Không đọc được ảnh vừa chụp")
            val target = File(scansDir, "$scanId.jpg")
            var quality = START_QUALITY
            var bytes: ByteArray
            do {
                bytes = ByteArrayOutputStream().use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                    stream.toByteArray()
                }
                quality -= QUALITY_STEP
            } while (bytes.size >= MAX_BYTES && quality >= MIN_QUALITY)
            bitmap.recycle()
            check(bytes.size < MAX_BYTES) { "Không thể nén ảnh xuống dưới 1 MB" }
            target.writeBytes(bytes)
            target
        } finally {
            source.delete() // dọn file tạm trong cacheDir
        }
    }

    fun toFileUri(file: File): String = Uri.fromFile(file).toString()

    fun fileFromUri(uri: String?): File? = uri?.let { Uri.parse(it).path }?.let { File(it) }?.takeIf { it.exists() }

    /** Đọc bytes để Base64 trong RAM khi gửi Gemini (không bao giờ lưu chuỗi Base64). */
    suspend fun readBytes(uri: String?): ByteArray? = withContext(ioDispatcher) { fileFromUri(uri)?.readBytes() }

    suspend fun delete(uri: String?) = withContext(ioDispatcher) { fileFromUri(uri)?.delete() }

    private fun decodeScaled(source: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null

        val longest = max(decoded.width, decoded.height)
        val scale = if (longest > MAX_SIDE) MAX_SIDE.toFloat() / longest else 1f
        val rotation = readRotation(source)
        if (scale == 1f && rotation == 0) return decoded

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (transformed != decoded) decoded.recycle()
        return transformed
    }

    private fun readRotation(file: File): Int = try {
        when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (error: Exception) {
        0
    }

    companion object {
        const val SCANS_DIR = "scans"
        const val MAX_SIDE = 1280
        const val MAX_BYTES = 1_000_000
        const val START_QUALITY = 80
        const val QUALITY_STEP = 10
        const val MIN_QUALITY = 30

        /** Dùng cho log/UI: kích thước KB, làm tròn. */
        fun sizeKb(file: File): Int = (file.length() / 1024f).roundToInt()
    }
}
