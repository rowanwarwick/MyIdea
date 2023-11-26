package com.example.myidea.ui.floating_window

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.myidea.databinding.FloatingWindowBinding
import com.example.myidea.ui.big_screen.BigScreen
import com.example.myidea.ui.capture_screen_service.CaptureScreenService
import com.example.myidea.ui.event_bus.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class FloatingWindow : AccessibilityService() {

    companion object {
        const val KOEF_SIZE = 0.55f
    }

    private lateinit var floatingWindowBinding: FloatingWindowBinding
    private lateinit var windowManager: WindowManager

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingWindowBinding = FloatingWindowBinding.inflate(inflater)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val (width, height) = loadSizeScreen()
        val layoutParams = createLayoutParams(width, height)
        windowManager.addView(floatingWindowBinding.root, layoutParams)
        floatingWindowBinding.test.setOnClickListener {
            disableSelf()
            startActivity(
                Intent(this, BigScreen::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        floatingWindowBinding.bStartCapture.setOnClickListener {
            startService(Intent(this, CaptureScreenService::class.java))
        }
        floatingWindowBinding.bStopCapture.setOnClickListener {
            takeScreenshot(Display.DEFAULT_DISPLAY, applicationContext.mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    Log.i("TAG", "success")
                    val bitmap = Bitmap.wrapHardwareBuffer(
                        screenshot.hardwareBuffer,
                        screenshot.colorSpace
                    )
                    floatingWindowBinding.test.setImageBitmap(bitmap)
                }

                override fun onFailure(errorCode: Int) {
                    Log.i("TAG", "error")
                }

            })
        }
        floatingWindowBinding.root.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> MoveFloatWindow.actionDown(layoutParams, event)
                MotionEvent.ACTION_MOVE -> {
                    windowManager.updateViewLayout(
                        floatingWindowBinding.root,
                        MoveFloatWindow.actionMove(layoutParams, event, width, height)
                    )
                }
            }
            false
        }
        CoroutineScope(Dispatchers.Default).launch {
            EventBus.listenCoordinateChannel().collect {
                Log.e("TAG", it.toString())
            }
        }

    }



    private fun getScreenshot() {

//        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        viewScreenBinding.root.draw(canvas)
//        return bitmap
//        mediaProjectionManager.getMediaProjection(RESULT_OK, Intent())
//        mediaProjection.createVirtualDisplay(
//            "ScreenCapture",
//            imageReader.width,
//            imageReader.height,
//            resources.displayMetrics.densityDpi,
//            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//            imageReader.surface,
//            null,
//            null
//        )
//        imageReader.setOnImageAvailableListener({ reader ->
//            val image: Image? = reader?.acquireLatestImage()
//            if (image != null) {
//                processImage(image)
//                image.close()
//            }
//        }, Handler(Looper.getMainLooper()))

    }

//    private fun processImage(image: Image) {
//        val planes: Array<Image.Plane> = image.planes
//        val buffer: ByteBuffer = planes[0].buffer
//        val pixelStride: Int = planes[0].pixelStride
//        val rowStride: Int = planes[0].rowStride
//        val rowPadding = rowStride - pixelStride * image.width
//
//        // Create Bitmap
//        val bitmap = Bitmap.createBitmap(
//            image.width + rowPadding / pixelStride,
//            image.height,
//            Bitmap.Config.ARGB_8888
//        )
//        bitmap.copyPixelsFromBuffer(buffer)
//        Log.e("TAG", "ok")
//    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingWindowBinding.root)
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