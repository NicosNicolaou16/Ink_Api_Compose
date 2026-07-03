package com.nicos.ink_api_compose.data.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.nicos.ink_api_compose.data.database.dao.StrokeDao
import com.nicos.ink_api_compose.data.database.entities.StrokeEntity
import javax.inject.Inject

@Database(
    entities = [StrokeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MyRoomDatabase : RoomDatabase() {
    abstract fun strokeDao(): StrokeDao

    @Inject
    internal lateinit var myDatabase: MyRoomDatabase

    companion object {
        private const val DB_NAME = "ink_database"
        private val LOCK = Any()

        operator fun invoke(context: Context) = buildDatabase(context)

        private fun buildDatabase(context: Context) = synchronized(LOCK) {
            Room.databaseBuilder(
                context.applicationContext,
                MyRoomDatabase::class.java,
                DB_NAME
            ).build()
        }
    }

    suspend fun deleteAll() {
        myDatabase.clearAllTables()
    }
}