package com.example.myidea.ui.model

internal data class CaptureCoordinate(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
) {
    override fun toString() = "startX: $startX, endX: $endX, startY: $startY, endY: $endY"
}