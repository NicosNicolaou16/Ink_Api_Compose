package com.nicos.ink_api_compose.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nicos.ink_api_compose.data.database.dao.StrokeDao
import com.nicos.ink_api_compose.data.database.entities.StrokeEntity
import com.nicos.ink_api_compose.data.database.type_converters.StrokeConverter
import javax.inject.Inject

@Database(
    entities = [StrokeEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(
    StrokeConverter::class
)
abstract class MyRoomDatabase : RoomDatabase() {
    abstract fun strokeDao(): StrokeDao

    @Inject
    internal lateinit var myDatabase: MyRoomDatabase

    companion object {
        private const val DB_NAME = "DB_NAME"
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

    fun deleteAll() {
        myDatabase.clearAllTables()
    }
}