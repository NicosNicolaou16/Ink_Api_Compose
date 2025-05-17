package com.nicos.ink_api_compose.data.repositories_impl

import com.nicos.ink_api_compose.data.database.MyRoomDatabase
import com.nicos.ink_api_compose.data.database.entities.StrokeEntity
import com.nicos.ink_api_compose.domain.StrokeRepository

class StrokeRepositoryImpl(
    private val myRoomDatabase: MyRoomDatabase
) : StrokeRepository {
    override suspend fun insertStroke(stroke: StrokeEntity) {

    }

    override suspend fun getStroke(): StrokeEntity {
        return myRoomDatabase.strokeDao().getStroke()
    }
}