package vn.edu.vlu.milestone2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import vn.edu.vlu.milestone2.data.local.entity.FavoriteProductEntity
import vn.edu.vlu.milestone2.data.local.entity.ProductEntity
import vn.edu.vlu.milestone2.data.local.model.FavoriteWithProduct

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE barcode = :barcode")
    suspend fun getProduct(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode")
    fun observeProduct(barcode: String): Flow<ProductEntity?>

    @Upsert
    suspend fun upsertProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE cached_at < :olderThan AND barcode NOT IN (SELECT product_barcode FROM favorite_products) AND barcode NOT IN (SELECT product_barcode FROM scan_results WHERE product_barcode IS NOT NULL)")
    suspend fun pruneCache(olderThan: Long): Int
}

@Dao
interface FavoriteDao {

    @Query(
        "SELECT p.*, f.added_at AS addedAt FROM favorite_products f " +
            "INNER JOIN products p ON p.barcode = f.product_barcode ORDER BY f.added_at DESC"
    )
    fun observeFavorites(): Flow<List<FavoriteWithProduct>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_products WHERE product_barcode = :barcode)")
    fun observeIsFavorite(barcode: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteProductEntity): Long

    @Query("DELETE FROM favorite_products WHERE product_barcode = :barcode")
    suspend fun delete(barcode: String): Int
}
