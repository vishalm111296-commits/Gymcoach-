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
 * Publishes remaining time via shared StateFlow (companion), consumed by the UI layer.
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

        fun start(context: Context, seconds: Int, nextSet: String = "") {
            val intent = Intent(context, RestTimerNotificationService::class.java)
                .setAction(ACTION_START)
                .setPackage(context.packageName)
                .putExtra(EXTRA_SECONDS, seconds)
                .putExtra(EXTRA_NEXT_SET, nextSet)
            context.startForegroundService(intent)
        }

        fun cancel(context: Context) {
            context.startService(
                Intent(context, RestTimerNotificationService::class.java)
                    .setAction(ACTION_CANCEL)
                    .setPackage(context.packageName)
            )
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
            ACTION_COMPLETE, ACTION_SKIP, ACTION_CANCEL -> finishTimer()
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int) {
        timer?.cancel()
        _remainingSeconds.value = seconds
        _isPaused.value = false
        timer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingSeconds.value = (millisUntilFinished / 1000L).toInt()
                updateNotification()
            }

            override fun onFinish() {
                finishTimer()
            }
        }.start()
    }

    private fun pauseTimer() {
        if (_isPaused.value) return
        timer?.cancel()
        timer = null
        _isPaused.value = true
        updateNotification()
    }

    private fun resumeTimer() {
        if (!_isPaused.value) return
        startTimer(_remainingSeconds.value)
    }

    private fun adjustTime(deltaSeconds: Int) {
        val newTotal = _remainingSeconds.value + deltaSeconds
        if (newTotal <= 0) {
            finishTimer()
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

    private fun finishTimer() {
        timer?.cancel()
        timer = null
        _remainingSeconds.value = 0
        _isPaused.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun promoteToForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
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
            Intent(this, MainActivity::class.java).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionPendingIntent(action: String, requestCode: Int): PendingIntent =
            PendingIntent.getBroadcast(
                this,
                requestCode,
                Intent(this, RestTimerReceiver::class.java)
                    .setAction(action)
                    .setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val text = buildString {
            append("REST ")
            append(formatSeconds(_remainingSeconds.value))
            if (nextSetLabel.isNotBlank()) {
                append(" — Next: ")
                append(nextSetLabel)
            }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Rest Timer")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Complete Set", actionPendingIntent(ACTION_COMPLETE, 1))
            .addAction(0, "Skip", actionPendingIntent(ACTION_SKIP, 2))
            .addAction(0, "+15s", actionPendingIntent(ACTION_PLUS_15, 3))
            .addAction(0, "-15s", actionPendingIntent(ACTION_MINUS_15, 4))
            .build()
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

    private fun formatSeconds(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
