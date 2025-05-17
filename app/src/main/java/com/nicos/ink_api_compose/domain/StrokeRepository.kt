package com.nicos.ink_api_compose.domain

import com.nicos.ink_api_compose.data.database.entities.StrokeEntity

interface StrokeRepository {
    suspend fun insertStroke(stroke: StrokeEntity)
    suspend fun getStroke(): StrokeEntity
}