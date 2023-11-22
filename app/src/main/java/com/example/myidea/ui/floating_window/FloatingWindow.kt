package com.example.myidea.ui.floating_window

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import com.example.myidea.ui.big_screen.BigScreen
import com.example.myidea.ui.capture_screen.CaptureScreen
import com.example.myidea.databinding.FloatingWindowBinding

internal class FloatingWindow : Service() {

    companion object {
        const val KOEF_SIZE = 0.55f
    }

    private lateinit var binding: FloatingWindowBinding
    private lateinit var windowManager: WindowManager

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = FloatingWindowBinding.inflate(inflater)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val (width, height) = loadSizeScreen()
        val layoutParams = createLayoutParams(width, height)
        windowManager.addView(binding.root, layoutParams)
        binding.test.setOnClickListener {
            stopSelf()
            startActivity(
                Intent(this, BigScreen::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        binding.bStartCapture.setOnClickListener {
            startService(Intent(this, CaptureScreen::class.java))
        }
        binding.bStopCapture.setOnClickListener {

        }
        binding.root.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> EventCoordinate.actionDown(layoutParams, event)
                MotionEvent.ACTION_MOVE -> {
                    windowManager.updateViewLayout(
                        binding.root,
                        EventCoordinate.actionMove(layoutParams, event, width, height)
                    )
                }
            }
            false
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(binding.root)
    }

    private fun loadSizeScreen(): Pair<Int, Int> =
        applicationContext.resources.displayMetrics.let { it.widthPixels to it.heightPixels }

    private fun createLayoutParams(width: Int, height: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            (KOEF_SIZE * width).toInt(),
            (KOEF_SIZE * height).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
            .also {
                it.gravity = Gravity.START or Gravity.TOP
                it.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

}