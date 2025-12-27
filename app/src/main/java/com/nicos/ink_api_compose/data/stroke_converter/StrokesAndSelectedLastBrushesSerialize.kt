package com.nicos.ink_api_compose.data.stroke_converter

import kotlinx.serialization.Serializable
import kotlin.collections.contentEquals
import kotlin.collections.contentHashCode
import kotlin.jvm.javaClass

@Serializable
data class StrokesAndSelectedLastBrushesSerialize(
    val strokeByteArray: ByteArray,
    val serializedBrush: SerializedBrush,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StrokesAndSelectedLastBrushesSerialize

        if (!strokeByteArray.contentEquals(other.strokeByteArray)) return false
        if (serializedBrush != other.serializedBrush) return false

        return true
    }

    override fun hashCode(): Int {
        var result = strokeByteArray.contentHashCode()
        result = 31 * result + serializedBrush.hashCode()
        return result
    }
}