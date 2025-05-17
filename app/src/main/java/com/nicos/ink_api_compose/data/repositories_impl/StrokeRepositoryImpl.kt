package com.nicos.ink_api_compose.data.repositories_impl

import com.nicos.ink_api_compose.data.database.entities.StrokeEntity
import com.nicos.ink_api_compose.domain.StrokeRepository

class StrokeRepositoryImpl: StrokeRepository  {
    override suspend fun insertStroke(stroke: StrokeEntity) {

    }

    override suspend fun deleteStroke(stroke: StrokeEntity) {

    }

    override suspend fun getStrokes(): List<StrokeEntity> {

    }
}