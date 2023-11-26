package com.example.myidea.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import com.example.myidea.ui.event_bus.EventBus
import com.example.myidea.ui.model.CaptureCoordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class CaptureView(context: Context, val invokeActionUp: () -> Unit = {}): View(context) {

    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f
    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                endX = event.x
                endY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                CoroutineScope(Dispatchers.Default).launch {
                    val slot = CaptureCoordinate(
                        startX.toInt(),
                        startY.toInt(),
                        endX.toInt() - startX.toInt(),
                        endY.toInt() - startY.toInt(),
                    )
                    EventBus.sendCoordinateChannel(slot)
                }
                invokeActionUp()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawRect(startX, startY, endX, endY, paint)
    }

}