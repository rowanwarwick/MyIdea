package com.example.myidea.ui.floating_window

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.core.app.NotificationCompat
import com.example.myidea.databinding.FloatingWindowBinding
import com.example.myidea.ui.big_screen.BigScreen
import com.example.myidea.ui.capture_screen_service.CaptureScreenService
import com.example.myidea.ui.event_bus.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

internal class FloatingWindow : Service() {

    companion object {
        const val KOEF_SIZE = 0.55f
        const val ID_NOTIFICATION = "foreground_service_channel"
        const val ID_CHANNEL = 1
    }

    private lateinit var floatingWindowBinding: FloatingWindowBinding
    private var windowManager: WindowManager? = null
    private var imageReader: ImageReader? = null
    private var mediaProjection: MediaProjection? = null
    private var isEnableCaptureScreen = false
    private var layoutParams: LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        val (width, height) =
            applicationContext.resources.displayMetrics.let { it.widthPixels to it.heightPixels }
        layoutParams = createLayoutParams(width, height)
        startForegroundNotification()
        addFloatingWindow()
        floatingWindowBinding.test.setOnClickListener { openBigScreen() }
        floatingWindowBinding.bStartCapture.setOnClickListener { captureScreen() }

        floatingWindowBinding.root.setOnTouchListener { _, event ->
            moveFloatingWindow(event, width, height)
        }
        setListenCoordinate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        intent?.let { token ->
            floatingWindowBinding.bStopCapture.setOnClickListener {
                getScreenshot(mediaProjectionManager, token)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager?.removeView(floatingWindowBinding.root)
        imageReader?.close()
        mediaProjection?.stop()
    }

    private fun createLayoutParams(width: Int, height: Int): LayoutParams =
        LayoutParams(
            (KOEF_SIZE * width).toInt(),
            (KOEF_SIZE * height).toInt(),
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
            .also {
                it.gravity = Gravity.START or Gravity.TOP
                it.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

    private fun startForegroundNotification() {
//        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        val channel = NotificationChannel(
//            ID_NOTIFICATION,
//            "Foreground Service Channel",
//            NotificationManager.IMPORTANCE_NONE
//        )
//            .apply { lockscreenVisibility = VISIBILITY_PRIVATE }
//        service.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, ID_NOTIFICATION)
            .setContentTitle("Foreground Service")
            .setContentText("Foreground service is running")
            .build()
        startForeground(ID_CHANNEL, notification)
    }

    private fun addFloatingWindow() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingWindowBinding = FloatingWindowBinding.inflate(inflater)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.addView(floatingWindowBinding.root, layoutParams)
    }

    private fun openBigScreen() {
        stopSelf()
        startActivity(
            Intent(this, BigScreen::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    private fun captureScreen() {
        startService(Intent(this, CaptureScreenService::class.java))
    }

    private fun getScreenshot(mediaProjectionManager: MediaProjectionManager, intent: Intent) {
        layoutParams?.let {
            if (isEnableCaptureScreen.not()) {
                mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, intent)
                startTakeScreenshot(it)
            } else {
                imageReader?.close()
                mediaProjection?.stop()
            }
            isEnableCaptureScreen = isEnableCaptureScreen.not()
        }
    }

    @SuppressLint("WrongConstant")
    private fun startTakeScreenshot(layoutParams: LayoutParams) {
        imageReader = ImageReader.newInstance(
            layoutParams.width,
            layoutParams.height,
            PixelFormat.RGBA_8888,
            1
        )
        mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            imageReader?.width ?: 1,
            imageReader?.height ?: 1,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                processImage(it)
                it.close()
            }}, Handler(Looper.getMainLooper())
        )
    }

    private fun processImage(image: Image) {
        val planes: Array<Image.Plane> = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride: Int = planes[0].pixelStride
        val rowStride: Int = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        floatingWindowBinding.test.setImageBitmap(bitmap)
    }

    private fun moveFloatingWindow(
        event: MotionEvent,
        width: Int,
        height: Int
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> layoutParams?.let { MoveFloatWindow.actionDown(it, event) }
            MotionEvent.ACTION_MOVE -> layoutParams?.let {
                windowManager?.updateViewLayout(
                    floatingWindowBinding.root,
                    MoveFloatWindow.actionMove(it, event, width, height)
                )
            }
        }
        return false
    }

    private fun setListenCoordinate() {
        CoroutineScope(Dispatchers.Default).launch {
            EventBus.listenCoordinateChannel().collect {
                Log.e("TAG", it.toString())
            }
        }
    }

}