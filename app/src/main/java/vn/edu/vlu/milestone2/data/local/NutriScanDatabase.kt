package vn.edu.vlu.milestone2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import vn.edu.vlu.milestone2.data.local.dao.HealthProfileDao
import vn.edu.vlu.milestone2.data.local.entity.HealthConditionEntity
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileEntity

@Database(
    entities = [HealthProfileEntity::class, HealthConditionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class NutriScanDatabase : RoomDatabase() {

    abstract fun healthProfileDao(): HealthProfileDao

    companion object {
        const val DATABASE_NAME = "nutriscan.db"

        @Volatile
        private var instance: NutriScanDatabase? = null

        fun getInstance(context: Context): NutriScanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NutriScanDatabase::class.java,
                    DATABASE_NAME
                ).build().also { database -> instance = database }
            }
    }
}
