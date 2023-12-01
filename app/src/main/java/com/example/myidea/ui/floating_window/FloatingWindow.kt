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
import com.example.myidea.ui.model.CaptureCoordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class FloatingWindow : Service() {

    companion object {
        const val KOEF_SIZE = 0.55f
        const val PARAMETER = "parameter"
        const val CHAR = "characteristics"
        const val PREFIX = "prefix"
        const val ONE = "one"
        const val TWO = "two"
        const val THREE = "three"
        const val FOUR = "four"
    }

    private val ID_NOTIFICATION = "foreground_service_channel"
    private val ID_CHANNEL = 1

    private lateinit var floatingWindowBinding: FloatingWindowBinding
    private var windowManager: WindowManager? = null
    private var imageReader: ImageReader? = null
    private var mediaProjection: MediaProjection? = null
    private var isEnableCaptureScreen = false
    private val mapCoordinate = mutableMapOf<String, CaptureCoordinate>()
    private val sizeDisplay: Pair<Int, Int> by lazy {
        applicationContext.resources.displayMetrics.let { it.widthPixels to it.heightPixels }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        val layoutParams = createLayoutParams()
        startForegroundNotification()
        addFloatingWindow(layoutParams)
        with(floatingWindowBinding) {
            bReturn.setOnClickListener { openBigScreen() }
            bCharField.setOnClickListener { captureScreen(CHAR) }
            bPrefixField.setOnClickListener { captureScreen(PREFIX) }
            bFirstField.setOnClickListener { captureScreen(ONE) }
            bSecondField.setOnClickListener { captureScreen(TWO) }
            bThirdField.setOnClickListener { captureScreen(THREE) }
            bFourField.setOnClickListener { captureScreen(FOUR) }
            root.setOnTouchListener { _, event -> moveFloatingWindow(event, layoutParams) }
        }
        setListenCoordinate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        intent?.let { token ->
            with(floatingWindowBinding) {
                ivCharField.setOnClickListener { getScreenshot(mediaProjectionManager, token) }
                ivPrefixField.setOnClickListener { getScreenshot(mediaProjectionManager, token) }
                ivFirstField.setOnClickListener { getScreenshot(mediaProjectionManager, token) }
                ivSecondField.setOnClickListener { getScreenshot(mediaProjectionManager, token) }
                ivThirdField.setOnClickListener { getScreenshot(mediaProjectionManager, token) }
                ivFourField.setOnClickListener { getScreenshot(mediaProjectionManager, token) }
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

    private fun createLayoutParams(): LayoutParams {
        val (width, height) = sizeDisplay
        return LayoutParams(
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

    private fun addFloatingWindow(layoutParams: LayoutParams) {
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

    private fun captureScreen(parameter: String) {
        val intent = Intent(this, CaptureScreenService::class.java).also {
            it.putExtra(PARAMETER, parameter)
        }
        startService(intent)
    }

    private fun getScreenshot(mediaProjectionManager: MediaProjectionManager, intent: Intent) {
        if (isEnableCaptureScreen.not()) {
            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, intent)
            startTakeScreenshot()
        } else {
            imageReader?.close()
            mediaProjection?.stop()
        }
        isEnableCaptureScreen = isEnableCaptureScreen.not()
    }

    @SuppressLint("WrongConstant")
    private fun startTakeScreenshot() {
        val (width, height) = sizeDisplay
        imageReader = ImageReader.newInstance(
            width,
            height,
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
        val planes = image.planes
        val buffer = planes[0].buffer
        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        for (entity in mapCoordinate) {
            val (parameter, coordinate) = entity
            val zone = Bitmap.createBitmap(bitmap, coordinate.startX, coordinate.startY, coordinate.width, coordinate.height)
            when (parameter) {
                CHAR -> floatingWindowBinding.ivCharField.setImageBitmap(zone)
                PREFIX -> floatingWindowBinding.ivPrefixField.setImageBitmap(zone)
                ONE -> floatingWindowBinding.ivFirstField.setImageBitmap(zone)
                TWO -> floatingWindowBinding.ivSecondField.setImageBitmap(zone)
                THREE -> floatingWindowBinding.ivThirdField.setImageBitmap(zone)
                FOUR -> floatingWindowBinding.ivFourField.setImageBitmap(zone)
            }
        }
    }

    private fun moveFloatingWindow(
        event: MotionEvent,
        layoutParams: LayoutParams
    ): Boolean {
        val (width, height) = sizeDisplay
        when (event.action) {
            MotionEvent.ACTION_DOWN -> MoveWindowHandler.actionDown(layoutParams, event)
            MotionEvent.ACTION_MOVE -> windowManager?.updateViewLayout(
                floatingWindowBinding.root,
                MoveWindowHandler.actionMove(layoutParams, event, width, height)
            )
        }
        return false
    }

    private fun setListenCoordinate() {
        CoroutineScope(Dispatchers.Default).launch {
            EventBus.listenCoordinateChannel().collect {
                val (parameter, coordinate) = it
                mapCoordinate[parameter] = coordinate
            }
        }
    }

}