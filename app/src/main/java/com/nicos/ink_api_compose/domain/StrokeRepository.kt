package com.nicos.ink_api_compose.domain

import com.nicos.ink_api_compose.data.entities.StrokeEntity

interface StrokeRepository {
    suspend fun insertStroke(stroke: StrokeEntity)
    suspend fun deleteStroke(stroke: StrokeEntity)
    suspend fun getStrokes(): List<StrokeEntity>
}