package com.gymcoach.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Relays notification action taps to [RestTimerNotificationService].
 * Exported=false; only the app's own PendingIntents reach it.
 */
class RestTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val supported = setOf(
            RestTimerNotificationService.ACTION_COMPLETE,
            RestTimerNotificationService.ACTION_SKIP,
            RestTimerNotificationService.ACTION_PLUS_15,
            RestTimerNotificationService.ACTION_MINUS_15,
            RestTimerNotificationService.ACTION_PAUSE,
            RestTimerNotificationService.ACTION_RESUME,
            RestTimerNotificationService.ACTION_CANCEL
        )
        if (action in supported) {
            context.startService(
                Intent(context, RestTimerNotificationService::class.java).setAction(action)
            )
        }
    }
}
