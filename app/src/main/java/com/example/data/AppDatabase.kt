package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Company::class,
        CategoryEntity::class,
        Product::class,
        SyncQueueEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun companyDao(): CompanyDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "showroom_manager_database"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * v2 -> v3: cloud sync metadata add hua (kuch delete nahi hota).
         * Purane rows ka updatedAt = 0 rakha jata hai taaki "cloud newer" jaisa
         * vyavhaar ho: agar cloud me docs hain to wo Room me aa jayenge, aur jo
         * rows cloud me nahi hain wo upload ho jayengi.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE companies ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE companies ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE categories ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        collectionName TEXT NOT NULL,
                        docId TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        attempts INTEGER NOT NULL
                    )"""
                )
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            suspend fun populateInitialData(database: AppDatabase) {
                database.companyDao().insertCompanies(InitialData.defaultCompanies)
                database.categoryDao().insertCategories(InitialData.defaultCategories)
                database.productDao().insertProducts(InitialData.getDefaultProducts())
            }
        }
    }
}
