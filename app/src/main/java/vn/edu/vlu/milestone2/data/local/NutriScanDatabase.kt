package vn.edu.vlu.milestone2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import vn.edu.vlu.milestone2.data.local.dao.FavoriteDao
import vn.edu.vlu.milestone2.data.local.dao.HealthProfileDao
import vn.edu.vlu.milestone2.data.local.dao.ProductDao
import vn.edu.vlu.milestone2.data.local.dao.RecipeDao
import vn.edu.vlu.milestone2.data.local.dao.ScanResultDao
import vn.edu.vlu.milestone2.data.local.entity.FavoriteProductEntity
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileEntity
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileItemEntity
import vn.edu.vlu.milestone2.data.local.entity.ProductEntity
import vn.edu.vlu.milestone2.data.local.entity.RecipeEntity
import vn.edu.vlu.milestone2.data.local.entity.ScanResultEntity

/**
 * NutriScan_DB – Room ORM (Milestone 07).
 * Singleton thread-safe bằng double-checked locking (@Volatile + synchronized).
 */
@Database(
    entities = [
        HealthProfileEntity::class,
        HealthProfileItemEntity::class,
        ProductEntity::class,
        FavoriteProductEntity::class,
        ScanResultEntity::class,
        RecipeEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NutriScanDatabase : RoomDatabase() {

    abstract fun healthProfileDao(): HealthProfileDao
    abstract fun productDao(): ProductDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun scanResultDao(): ScanResultDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        const val DATABASE_NAME = "NutriScan_DB"

        @Volatile
        private var instance: NutriScanDatabase? = null

        fun getInstance(context: Context): NutriScanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NutriScanDatabase::class.java,
                    DATABASE_NAME
                )
                    // Schema v1 (Milestone 07 bản đầu) chỉ chứa dữ liệu thử nghiệm → cho phép tạo lại.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
