package com.example.myidea.ui.capture_screen_service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import com.example.myidea.ui.common.CaptureView

internal class CaptureScreenService: Service() {

    private lateinit var windowManager: WindowManager
    private var captureView: CaptureView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        captureView = CaptureView(this, ::actionUp)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.START or Gravity.TOP
        windowManager.addView(captureView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(captureView)
    }

    private fun actionUp() {
        stopSelf()
    }

}
