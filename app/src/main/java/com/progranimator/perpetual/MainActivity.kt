package com.progranimator.perpetual

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager

/**
 * PERPETUAL — a permanent shell around one frozen pixel-art progress bar.
 * Everything around the bar is cartridge-driven and hot-swaps from the
 * shared Google Drive folder; the bar's graphics never change.
 */
class MainActivity : Activity() {

    private lateinit var stage: StageView
    private val handler = Handler(Looper.getMainLooper())

    private val poll = object : Runnable {
        override fun run() {
            checkUpdate()
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        stage = StageView(this)
        stage.pixelFont = try {
            Typeface.createFromAsset(assets, "fonts/PressStart2P-Regular.ttf")
        } catch (_: Exception) {
            Typeface.MONOSPACE
        }
        setContentView(stage)
        applyCartridge()
    }

    private fun applyCartridge() {
        val c = CartridgeLoader.cachedCartridge(this) ?: return
        stage.assetDir = CartridgeLoader.assetDir(this)
        stage.cartridge = c
    }

    private fun checkUpdate() {
        Thread {
            val updated =
                CartridgeLoader.checkForUpdate(this, BuildConfig.MANIFEST_URL)
            if (updated) handler.post { applyCartridge() }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(poll)
        checkUpdate() // instant refresh on open
        handler.postDelayed(poll, POLL_MS)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(poll)
    }

    companion object {
        private const val POLL_MS = 60_000L
    }
}
