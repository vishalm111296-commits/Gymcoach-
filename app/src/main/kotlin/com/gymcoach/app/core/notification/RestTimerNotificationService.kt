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
import kotlinx.coroutines.flow.StateFlow

/**
 * Foreground service that owns the rest timer so it keeps ticking with the screen off.
 * Uses official FOREGROUND_SERVICE_TYPE_SPECIAL_USE on Android 14+ (API 34+).
 * Subtype property declared in manifest: android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE.
 * Delegates state transitions to [RestTimerStateMachine].
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
        const val ACTION_ADJUST = "com.gymcoach.app.resttimer.ADJUST"
        const val ACTION_RESTORE = "com.gymcoach.app.resttimer.RESTORE"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_NEXT_SET = "extra_next_set"
        const val EXTRA_WORKOUT_ID = "extra_workout_id"
        const val EXTRA_DELTA = "extra_delta"

        private val stateMachine = RestTimerStateMachine()
        private val audioCoach = com.gymcoach.app.core.audio.RestAudioCoach(com.gymcoach.app.core.audio.RestAudioCueEvaluator())

        val remainingSeconds: StateFlow<Int> = stateMachine.remainingSeconds
        val isPaused: StateFlow<Boolean> = stateMachine.isPaused
        val isRunning: StateFlow<Boolean> = stateMachine.isRunning
        val totalDurationSeconds: StateFlow<Int> = stateMachine.totalDurationSeconds
        val workoutId: Long get() = stateMachine.workoutId
        val nextSetLabel: String get() = stateMachine.nextSetLabel

        fun start(context: Context, seconds: Int, nextSet: String = "", workoutId: Long = -1L) {
            val safeSeconds = seconds.coerceAtLeast(0)
            val now = System.currentTimeMillis()
            val endMillis = now + safeSeconds * 1000L
            RestTimerPreferences.save(
                context,
                DurableTimerState(
                    isRunning = safeSeconds > 0,
                    isPaused = false,
                    restEndEpochMillis = endMillis,
                    totalDurationSeconds = safeSeconds,
                    pausedRemainingSeconds = safeSeconds,
                    nextSetLabel = nextSet,
                    workoutId = workoutId
                )
            )
            val intent = Intent(context, RestTimerNotificationService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_SECONDS, seconds)
                .putExtra(EXTRA_NEXT_SET, nextSet)
                .putExtra(EXTRA_WORKOUT_ID, workoutId)
            context.startForegroundService(intent)
        }

        fun pause(context: Context) {
            val durable = RestTimerPreferences.load(context)
            if (durable.isRunning && !durable.isPaused) {
                val remaining = durable.calculateRemainingSeconds()
                RestTimerPreferences.save(context, durable.copy(isPaused = true, pausedRemainingSeconds = remaining))
            }
            context.startService(
                Intent(context, RestTimerNotificationService::class.java).setAction(ACTION_PAUSE)
            )
        }

        fun resume(context: Context) {
            val durable = RestTimerPreferences.load(context)
            if (durable.isRunning && durable.isPaused) {
                val endMillis = System.currentTimeMillis() + durable.pausedRemainingSeconds * 1000L
                RestTimerPreferences.save(context, durable.copy(isPaused = false, restEndEpochMillis = endMillis))
            }
            context.startService(
                Intent(context, RestTimerNotificationService::class.java).setAction(ACTION_RESUME)
            )
        }

        fun adjust(context: Context, deltaSeconds: Int) {
            val durable = RestTimerPreferences.load(context)
            if (durable.isRunning) {
                if (durable.isPaused) {
                    val newRem = (durable.pausedRemainingSeconds + deltaSeconds).coerceAtLeast(0)
                    if (newRem == 0) {
                        cancel(context)
                        return
                    }
                    RestTimerPreferences.save(context, durable.copy(pausedRemainingSeconds = newRem, totalDurationSeconds = maxOf(durable.totalDurationSeconds, newRem)))
                } else {
                    val newEnd = durable.restEndEpochMillis + deltaSeconds * 1000L
                    val newRem = ((newEnd - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)
                    if (newRem == 0) {
                        cancel(context)
                        return
                    }
                    RestTimerPreferences.save(context, durable.copy(restEndEpochMillis = newEnd, totalDurationSeconds = maxOf(durable.totalDurationSeconds, newRem)))
                }
            }
            context.startService(
                Intent(context, RestTimerNotificationService::class.java)
                    .setAction(ACTION_ADJUST)
                    .putExtra(EXTRA_DELTA, deltaSeconds)
            )
        }

        fun cancel(context: Context) {
            RestTimerPreferences.clear(context)
            context.startService(
                Intent(context, RestTimerNotificationService::class.java).setAction(ACTION_CANCEL)
            )
        }

        fun restore(context: Context) {
            val durable = RestTimerPreferences.load(context)
            if (durable.isRunning) {
                val rem = durable.calculateRemainingSeconds()
                if (rem > 0) {
                    val intent = Intent(context, RestTimerNotificationService::class.java).setAction(ACTION_RESTORE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } else {
                    RestTimerPreferences.clear(context)
                }
            }
        }

        fun formatSeconds(totalSeconds: Int): String =
            RestTimerStateMachine.formatSeconds(totalSeconds)
    }

    private var timer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 90)
                val nextSet = intent.getStringExtra(EXTRA_NEXT_SET) ?: ""
                val workoutId = intent.getLongExtra(EXTRA_WORKOUT_ID, -1L)
                startTimer(seconds, nextSet, workoutId)
                promoteToForeground()
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_PLUS_15 -> adjustTime(15)
            ACTION_MINUS_15 -> adjustTime(-15)
            ACTION_ADJUST -> {
                val delta = intent.getIntExtra(EXTRA_DELTA, 0)
                adjustTime(delta)
            }
            ACTION_RESTORE, null -> restoreTimer()
            ACTION_COMPLETE, ACTION_SKIP -> finishTimer(isCompleted = true)
            ACTION_CANCEL -> finishTimer(isCompleted = false)
        }
        return START_NOT_STICKY
    }

    private fun restoreTimer() {
        val saved = RestTimerPreferences.load(this)
        if (saved.isRunning) {
            val restored = stateMachine.restore(saved)
            if (restored) {
                promoteToForeground()
                if (!saved.isPaused) {
                    startTimer(stateMachine.remainingSeconds.value, stateMachine.nextSetLabel, stateMachine.workoutId)
                } else {
                    updateNotification()
                }
            } else {
                finishTimer(isCompleted = true)
            }
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startTimer(
        seconds: Int,
        nextSet: String = stateMachine.nextSetLabel,
        workoutId: Long = stateMachine.workoutId
    ) {
        timer?.cancel()
        stateMachine.start(seconds, nextSet, workoutId)
        if (!stateMachine.isRunning.value) {
            finishTimer(isCompleted = false)
            return
        }
        timer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000L).toInt()
                stateMachine.tick(sec)
                audioCoach.onTick(sec)
                updateNotification()
            }

            override fun onFinish() {
                finishTimer(isCompleted = true)
            }
        }.start()
    }

    private fun pauseTimer() {
        if (!stateMachine.isRunning.value || stateMachine.isPaused.value) return
        timer?.cancel()
        timer = null
        stateMachine.pause()
        val durable = RestTimerPreferences.load(this)
        if (durable.isRunning) {
            RestTimerPreferences.save(this, durable.copy(isPaused = true, pausedRemainingSeconds = stateMachine.remainingSeconds.value))
        }
        updateNotification()
    }

    private fun resumeTimer() {
        if (!stateMachine.isRunning.value || !stateMachine.isPaused.value) return
        stateMachine.resume()
        val rem = stateMachine.remainingSeconds.value
        val durable = RestTimerPreferences.load(this)
        if (durable.isRunning) {
            val endMillis = System.currentTimeMillis() + rem * 1000L
            RestTimerPreferences.save(this, durable.copy(isPaused = false, restEndEpochMillis = endMillis))
        }
        startTimer(rem)
    }

    private fun adjustTime(deltaSeconds: Int) {
        if (!stateMachine.isRunning.value) return
        val newTotal = stateMachine.adjust(deltaSeconds)
        if (newTotal <= 0) {
            finishTimer(isCompleted = false)
            return
        }
        val durable = RestTimerPreferences.load(this)
        if (durable.isRunning) {
            if (stateMachine.isPaused.value) {
                RestTimerPreferences.save(
                    this,
                    durable.copy(
                        pausedRemainingSeconds = newTotal,
                        totalDurationSeconds = maxOf(durable.totalDurationSeconds, newTotal)
                    )
                )
            } else {
                val newEnd = System.currentTimeMillis() + newTotal * 1000L
                RestTimerPreferences.save(
                    this,
                    durable.copy(
                        restEndEpochMillis = newEnd,
                        totalDurationSeconds = maxOf(durable.totalDurationSeconds, newTotal)
                    )
                )
            }
        }
        if (stateMachine.isPaused.value) {
            updateNotification()
        } else {
            startTimer(newTotal)
            promoteToForeground()
        }
    }

    private fun finishTimer(isCompleted: Boolean = false) {
        RestTimerPreferences.clear(this)
        if (isCompleted) {
            triggerCompletionHaptics()
            audioCoach.onComplete()
            stateMachine.complete()
        } else {
            stateMachine.cancel()
        }
        timer?.cancel()
        timer = null
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
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
            }
        } catch (_: Exception) {
            // Non-critical fallback if vibration is not supported by environment
        }
    }

    private fun promoteToForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
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

        val isPaused = stateMachine.isPaused.value
        val remaining = stateMachine.remainingSeconds.value
        val nextSet = stateMachine.nextSetLabel

        val text = buildString {
            append("REST ")
            append(formatSeconds(remaining))
            if (isPaused) {
                append(" (PAUSED)")
            }
            if (nextSet.isNotBlank()) {
                append(" — Next: ")
                append(nextSet)
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Rest Timer")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOnlyAlertOnce(true)
            .setOngoing(!isPaused)
            .setContentIntent(contentIntent)

        if (!isPaused && remaining > 0) {
            builder.setWhen(System.currentTimeMillis() + remaining * 1000L)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        } else {
            builder.setUsesChronometer(false)
        }

        if (isPaused) {
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
        stateMachine.reset()
    }
}
