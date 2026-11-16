package vn.edu.vlu.milestone2.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 1 lần quét (mã vạch hoặc ảnh nhãn).
 *
 * Ràng buộc của giảng viên: [imagePath] CHỈ lưu file URI (file:///…/files/scans/{id}.jpg) trỏ tới
 * ảnh JPEG đã nén < 1 MB. Bảng này KHÔNG có cột Base64 / ByteArray nào → không thể gặp
 * SQLiteBlobTooBigException khi đọc lịch sử.
 */
@Entity(
    tableName = "scan_results",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["barcode"],
            childColumns = ["product_barcode"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["product_barcode"]), Index(value = ["status"]), Index(value = ["created_at"])]
)
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "scan_id") val scanId: Long = 0,
    @ColumnInfo(name = "product_barcode") val productBarcode: String?,
    @ColumnInfo(name = "image_path") val imagePath: String?,
    @ColumnInfo(name = "product_name") val productName: String,
    /** JSON array các thành phần (List<String>). */
    @ColumnInfo(name = "ingredients_json") val ingredientsJson: String,
    val rating: Rating?,
    /** JSON trả về từ Gemini, đã được kiểm tra theo responseSchema. */
    @ColumnInfo(name = "analysis_json") val analysisJson: String?,
    val note: String?,
    val status: ScanStatus,
    val source: ScanSource,
    @ColumnInfo(name = "error_message") val errorMessage: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
