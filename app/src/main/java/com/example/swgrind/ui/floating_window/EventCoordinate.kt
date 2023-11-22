package com.example.swgrind.ui.floating_window

import android.view.MotionEvent
import android.view.WindowManager

internal object EventCoordinate {

    private var x: Int = 0
    private var y: Int = 0
    private var px: Float = 0f
    private var py: Float = 0f

    fun actionDown(layoutParams: WindowManager.LayoutParams, event: MotionEvent) {
        x = layoutParams.x
        y = layoutParams.y
        px = event.rawX
        py = event.rawY
    }

    fun actionMove(
        layoutParams: WindowManager.LayoutParams,
        event: MotionEvent,
        width: Int,
        height: Int,
    ): WindowManager.LayoutParams {
        val changeX = x + (event.rawX - px).toInt()
        val changeY = y + (event.rawY - py).toInt()
        val maxX = ((1 - FloatingWindow.KOEF_SIZE) * width).toInt()
        val maxY = ((1 - FloatingWindow.KOEF_SIZE) * height).toInt()
        layoutParams.x = changeX.coerceIn(0, maxX)
        layoutParams.y = changeY.coerceIn(0, maxY)
        return layoutParams
    }

}
