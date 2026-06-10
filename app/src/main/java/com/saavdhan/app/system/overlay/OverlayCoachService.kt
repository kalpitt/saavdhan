package com.saavdhan.app.system.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.saavdhan.app.R

/**
 * Draws a small banner on top of whatever is on screen (e.g. the system Settings list), showing the
 * user the next step. Needs the "draw over other apps" permission (`SYSTEM_ALERT_WINDOW`); the
 * caller checks that via [OverlayCoach]. The banner stays until the user taps "Got it". See ADR-0008.
 */
class OverlayCoachService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra(EXTRA_MESSAGE)
        if (message.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        showOverlay(message)
        return START_NOT_STICKY
    }

    private fun showOverlay(message: String) {
        removeOverlay()
        val wm = getSystemService(WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm

        val view = buildBanner(message)
        overlayView = view

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP
        }

        try {
            wm.addView(view, params)
        } catch (e: Exception) {
            // If the OS refuses (e.g. permission revoked between check and now), fail quietly.
            stopSelf()
        }
    }

    private fun buildBanner(message: String): View {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF00695C.toInt()) // brand teal
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val messageView = TextView(this).apply {
            text = message
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val doneButton = Button(this).apply {
            text = getString(R.string.coach_done)
            setOnClickListener { stopSelf() }
        }
        card.addView(messageView)
        card.addView(doneButton)
        return card
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        try {
            windowManager?.removeView(view)
        } catch (e: Exception) {
            // already removed; ignore
        }
        overlayView = null
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MESSAGE = "extra_message"
    }
}
