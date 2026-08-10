package com.automatelinux.hotPotato.overlay

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.IBinder
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.automatelinux.hotPotato.MainActivity
import com.automatelinux.hotPotato.hp.ApiClient
import com.automatelinux.hotPotato.hp.Claim
import com.automatelinux.hotPotato.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class OverlayService : Service() {

    companion object {
        const val ACTION_STOP = "com.automatelinux.hotPotato.STOP_OVERLAY"
        private const val CHANNEL_ID = "hp_overlay"
        private const val NOTIF_ID = 1

        @Volatile
        var running = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val uiState = MutableStateFlow<UiState>(UiState.Loading)
    private val expanded = MutableStateFlow(false)
    private val actionError = MutableStateFlow<String?>(null)
    private var lastRemaining: Int? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        addOverlay()
        scope.launch { pollLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        lifecycleOwner.destroy()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "בועת סיבוב", NotificationManager.IMPORTANCE_LOW),
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle("תפוח אדמה חם — סיבוב פעיל")
            .setContentIntent(openIntent)
            .addAction(0, "סגור בועה", stopIntent)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this, NOTIF_ID, notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun addOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 300
        }
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                AppTheme {
                    BubbleRoot(uiState, expanded, actionError, actions)
                }
            }
        }
        lifecycleOwner.create()
        windowManager.addView(view, params)
        overlayView = view
        overlayParams = params
    }

    private suspend fun pollLoop() {
        while (true) {
            try {
                val state = ApiClient.getState()
                uiState.value = UiState.Data(state)
                val remaining = state.remaining
                if (state.sale?.status == "active" &&
                    lastRemaining != null && lastRemaining!! > 0 && remaining <= 0
                ) {
                    soldOutAlarm()
                }
                lastRemaining = remaining
            } catch (e: Exception) {
                uiState.value = UiState.Error(e.message ?: "network error")
            }
            delay(4_000)
        }
    }

    private fun refreshNow() {
        scope.launch {
            try {
                uiState.value = UiState.Data(ApiClient.getState())
            } catch (e: Exception) {
                uiState.value = UiState.Error(e.message ?: "network error")
            }
        }
    }

    private fun soldOutAlarm() {
        expanded.value = true
        scope.launch {
            val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            try {
                repeat(4) {
                    tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
                    delay(500)
                }
            } finally {
                tone.release()
            }
        }
        val vibrator = (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 400, 150, 400, 150, 700), -1),
        )
    }

    private fun act(block: suspend () -> Unit) {
        scope.launch {
            try {
                actionError.value = null
                block()
                refreshNow()
            } catch (e: Exception) {
                // Never swallow a failed tap silently (veggieBox issue #17 lesson).
                actionError.value = e.message ?: "הפעולה נכשלה"
            }
        }
    }

    private val actions = object : BubbleActions {
        override fun onToggleExpanded() {
            expanded.value = !expanded.value
        }

        override fun onDrag(dx: Int, dy: Int) {
            val params = overlayParams ?: return
            val view = overlayView ?: return
            params.x += dx
            params.y += dy
            windowManager.updateViewLayout(view, params)
        }

        override fun onCall(phone: String) {
            val granted = ContextCompat.checkSelfPermission(
                this@OverlayService, Manifest.permission.CALL_PHONE,
            ) == PackageManager.PERMISSION_GRANTED
            // Permission granted → dial immediately; not granted → open the
            // dialer pre-filled (explicit per-state behavior, not a fallback).
            val action = if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL
            startActivity(
                Intent(action, Uri.parse("tel:$phone"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }

        override fun onConfirm(claim: Claim, qty: Int) = act {
            ApiClient.actOnClaim(claim.id, "confirm", qty = qty)
        }

        override fun onReject(claim: Claim) = act {
            ApiClient.actOnClaim(claim.id, "reject")
        }

        override fun onDelivered(claim: Claim, paid: Boolean) = act {
            ApiClient.actOnClaim(claim.id, "delivered", paid = paid)
        }

        override fun onSetStop(claim: Claim) = act {
            ApiClient.setCurrentStop(claim.chatJid)
        }

        override fun onAdjust(delta: Int) = act {
            ApiClient.adjust(delta)
        }

        override fun onEndSale() = act {
            ApiClient.endSale()
        }

        override fun onCloseBubble() {
            stopSelf()
        }
    }
}
