package com.gymcoach.app.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gymcoach.app.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service that owns the rest timer so it keeps ticking with the screen off.
 * Uses official FOREGROUND_SERVICE_TYPE_HEALTH on Android 14+ (API 34+) to eliminate
 * the 3-minute hard ceiling of shortService.
 * Publishes remaining time, pause state, and running state via companion StateFlows.
 */
class RestTimerNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "rest_timer_channel"
        const val NOTIFICATION_ID = 7777

        const val ACTION_START = "com.gymcoach.app.resttimer.START"
        const val ACTION_PAUSE = "com.gymcoach.app.resttimer.PAUSE"
        const val ACTION_RESUME = "com.gymcoach.app.resttimer.RESUME"
        const val ACTION_CANCEL = "com.gymcoach.app.resttimer.CANCEL"
        const val ACTION_COMPLETE = "com.gymcoach.app.resttimer.COMPLETE"
        const val ACTION_SKIP = "com.gymcoach.app.resttimer.SKIP"
        const val ACTION_PLUS_15 = "com.gymcoach.app.resttimer.PLUS_15"
        const val ACTION_MINUS_15 = "com.gymcoach.app.resttimer.MINUS_15"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_NEXT_SET = "extra_next_set"

        // Shared state: survives config changes, observable from ViewModel/Compose.
        private val _remainingSeconds = MutableStateFlow(0)
        val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context, seconds: Int, nextSet: String = "") {
            val intent = Intent(context, RestTimerNotificationService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_SECONDS, seconds)
                .putExtra(EXTRA_NEXT_SET, nextSet)
            context.startForegroundService(intent)
        }

        fun pause(context: Context) {
            context.startService(
                Intent(context, RestTimerNotificationService::class.java).setAction(ACTION_PAUSE)
            )
        }

        fun resume(context: Context) {
            context.startService(
                Intent(context, RestTimerNotificationService::class.java).setAction(ACTION_RESUME)
            )
        }

        fun adjust(context: Context, deltaSeconds: Int) {
            val action = if (deltaSeconds >= 0) ACTION_PLUS_15 else ACTION_MINUS_15
            context.startService(
                Intent(context, RestTimerNotificationService::class.java).setAction(action)
            )
        }

        fun cancel(context: Context) {
            context.startService(
                Intent(context, RestTimerNotificationService::class.java).setAction(ACTION_CANCEL)
            )
        }

        fun formatSeconds(totalSeconds: Int): String {
            val safeSec = totalSeconds.coerceAtLeast(0)
            val minutes = safeSec / 60
            val seconds = safeSec % 60
            return "%02d:%02d".format(minutes, seconds)
        }

        internal fun updateStateForTesting(remaining: Int, paused: Boolean, running: Boolean) {
            _remainingSeconds.value = remaining
            _isPaused.value = paused
            _isRunning.value = running
        }

        internal fun resetStateForTesting() {
            _remainingSeconds.value = 0
            _isPaused.value = false
            _isRunning.value = false
        }
    }

    private var timer: CountDownTimer? = null
    private var nextSetLabel: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                nextSetLabel = intent.getStringExtra(EXTRA_NEXT_SET) ?: ""
                startTimer(intent.getIntExtra(EXTRA_SECONDS, 90))
                promoteToForeground()
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_PLUS_15 -> adjustTime(15)
            ACTION_MINUS_15 -> adjustTime(-15)
            ACTION_COMPLETE, ACTION_SKIP -> finishTimer(isCompleted = true)
            ACTION_CANCEL -> finishTimer(isCompleted = false)
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int) {
        timer?.cancel()
        _remainingSeconds.value = seconds
        _isPaused.value = false
        _isRunning.value = true
        timer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingSeconds.value = (millisUntilFinished / 1000L).toInt()
                updateNotification()
            }

            override fun onFinish() {
                finishTimer(isCompleted = true)
            }
        }.start()
    }

    private fun pauseTimer() {
        if (!_isRunning.value || _isPaused.value) return
        timer?.cancel()
        timer = null
        _isPaused.value = true
        updateNotification()
    }

    private fun resumeTimer() {
        if (!_isRunning.value || !_isPaused.value) return
        startTimer(_remainingSeconds.value)
    }

    private fun adjustTime(deltaSeconds: Int) {
        if (!_isRunning.value) return
        val newTotal = _remainingSeconds.value + deltaSeconds
        if (newTotal <= 0) {
            finishTimer(isCompleted = false)
            return
        }
        if (_isPaused.value) {
            _remainingSeconds.value = newTotal
            updateNotification()
        } else {
            startTimer(newTotal)
            promoteToForeground()
        }
    }

    private fun finishTimer(isCompleted: Boolean = false) {
        if (isCompleted) {
            triggerCompletionHaptics()
        }
        timer?.cancel()
        timer = null
        _remainingSeconds.value = 0
        _isPaused.value = false
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun triggerCompletionHaptics() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 200, 100, 200), -1)
                }
            }
        } catch (_: Exception) {
            // Non-critical fallback if vibration is not supported by environment
        }
    }

    private fun promoteToForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionPendingIntent(action: String, requestCode: Int): PendingIntent =
            PendingIntent.getBroadcast(
                this,
                requestCode,
                Intent(this, RestTimerReceiver::class.java).setAction(action).setPackage(this.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val text = buildString {
            append("REST ")
            append(formatSeconds(_remainingSeconds.value))
            if (_isPaused.value) {
                append(" (PAUSED)")
            }
            if (nextSetLabel.isNotBlank()) {
                append(" — Next: ")
                append(nextSetLabel)
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Rest Timer")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOnlyAlertOnce(true)
            .setOngoing(!_isPaused.value)
            .setContentIntent(contentIntent)

        if (_isPaused.value) {
            builder.addAction(0, "Resume", actionPendingIntent(ACTION_RESUME, 1))
        } else {
            builder.addAction(0, "Pause", actionPendingIntent(ACTION_PAUSE, 1))
        }

        builder.addAction(0, "+15s", actionPendingIntent(ACTION_PLUS_15, 2))
            .addAction(0, "-15s", actionPendingIntent(ACTION_MINUS_15, 3))
            .addAction(0, "Skip", actionPendingIntent(ACTION_SKIP, 4))

        return builder.build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Rest Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Rest timer countdown during workout sessions"
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
        _remainingSeconds.value = 0
        _isPaused.value = false
        _isRunning.value = false
    }
}
