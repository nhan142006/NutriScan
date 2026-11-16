package vn.edu.vlu.milestone2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import vn.edu.vlu.milestone2.data.local.entity.ScanResultEntity
import vn.edu.vlu.milestone2.data.local.entity.ScanStatus

@Dao
interface ScanResultDao {

    @Query("SELECT * FROM scan_results ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results ORDER BY created_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE scan_id = :scanId")
    fun observe(scanId: Long): Flow<ScanResultEntity?>

    @Query("SELECT * FROM scan_results WHERE scan_id = :scanId")
    suspend fun get(scanId: Long): ScanResultEntity?

    @Query("SELECT * FROM scan_results WHERE scan_id IN (:scanIds)")
    suspend fun getByIds(scanIds: List<Long>): List<ScanResultEntity>

    /** Dữ liệu đầu vào cho module Công thức: N lần quét gần nhất đã phân tích xong (SUITABLE/LIMIT). */
    @Query(
        "SELECT * FROM scan_results WHERE status = 'DONE' AND rating IN ('SUITABLE','LIMIT') " +
            "ORDER BY created_at DESC LIMIT :limit"
    )
    suspend fun getRecentUsable(limit: Int): List<ScanResultEntity>

    /** Các lần quét bị đánh giá AVOID – dùng làm danh sách loại trừ khi gợi ý món. */
    @Query("SELECT * FROM scan_results WHERE status = 'DONE' AND rating = 'AVOID' ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentAvoided(limit: Int): List<ScanResultEntity>

    @Query("SELECT COUNT(*) FROM scan_results WHERE status = :status")
    fun observeCount(status: ScanStatus): Flow<Int>

    @Query("SELECT scan_id FROM scan_results WHERE status = 'PENDING'")
    suspend fun getPendingIds(): List<Long>

    @Insert
    suspend fun insert(scan: ScanResultEntity): Long

    @Update
    suspend fun update(scan: ScanResultEntity): Int

    @Query("UPDATE scan_results SET note = :note WHERE scan_id = :scanId")
    suspend fun updateNote(scanId: Long, note: String?): Int

    @Query("UPDATE scan_results SET image_path = :imagePath WHERE scan_id = :scanId")
    suspend fun updateImagePath(scanId: Long, imagePath: String?): Int

    @Query("UPDATE scan_results SET status = :status, error_message = :error WHERE scan_id = :scanId")
    suspend fun updateStatus(scanId: Long, status: ScanStatus, error: String?): Int

    @Query("DELETE FROM scan_results WHERE scan_id = :scanId")
    suspend fun delete(scanId: Long): Int
}
