package com.example.myidea.ui.model

internal data class CaptureCoordinate(
    val startX: Int,
    val startY: Int,
    val width: Int,
    val height: Int,
) {
    override fun toString() = "startX: $startX, startY: $startY, width: $width, height: $height"
}