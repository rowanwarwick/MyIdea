package com.example.myidea.ui.media_token

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.example.myidea.ui.floating_window.FloatingWindow

internal class MediaToken : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val resultRequestPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val intent = Intent(this, FloatingWindow::class.java)
                it.data?.let { data -> intent.putExtras(data) }
                startService(intent)
            }
            finish()
        }
        resultRequestPermission.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

}