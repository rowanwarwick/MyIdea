package com.example.myidea.ui.big_screen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.myidea.databinding.BigScreenBinding
import com.example.myidea.ui.floating_window.FloatingWindow
import com.example.myidea.ui.media_token.MediaToken

internal class BigScreen : AppCompatActivity() {

    private var resultRequestPermission: ActivityResultLauncher<Intent>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = BigScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        resultRequestPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
        binding.button.setOnClickListener {
            if (canDrawOverlays()) {
                startActivity(Intent(this@BigScreen, MediaToken::class.java))
                finish()
            } else {
                requestPermission()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        stopService(Intent(this@BigScreen, FloatingWindow::class.java))
    }

    private fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(this)

    private fun requestPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        resultRequestPermission?.launch(intent)
    }

}