package com.example.myidea.ui.capture_screen_service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import com.example.myidea.ui.common.CaptureView
import com.example.myidea.ui.event_bus.EventBus
import com.example.myidea.ui.floating_window.FloatingWindow.Companion.PARAMETER
import com.example.myidea.ui.model.CaptureCoordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class CaptureScreenService: Service() {

    private val windowManager: WindowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private var captureView: CaptureView? = null
    private var parameter: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        parameter = intent?.getStringExtra(PARAMETER)
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.Default).launch {
            EventBus.listenStopDrawChannel().collect { sendCoordinate() }
        }
        captureView = CaptureView(this)
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
        captureView?.let { windowManager.removeView(it) }
    }

    private suspend fun sendCoordinate() {
        captureView?.let {
            val slot = CaptureCoordinate(
                startX = it.startX.toInt(),
                startY = it.startY.toInt(),
                width = it.endX.toInt() - it.startX.toInt(),
                height = it.endY.toInt() - it.startY.toInt(),
            )
            parameter?.let { EventBus.sendCoordinateChannel(it to slot) }
        }
        stopSelf()
    }

}
