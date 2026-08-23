## TIMER

**Current state: ARCHITECTURAL CONCERNS.**

**RestTimerNotificationService** audit findings:

- **shortService type** has ~6min execution window on API 34+; long rests near cap need verification
- **Foreground service** with CountDownTimer, ongoing low-priority notification (channel `rest_timer_channel`, id 7777)
- **Complete/Skip/+15/-15 actions** with shared StateFlow for remaining time + pause state
- **RestTimerReceiver** relays notification action broadcasts to service
- **Manifest**: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SHORT_SERVICE`, `POST_NOTIFICATIONS`; service (shortService type) + receiver registered
- **No production caller** of timer completion logic found; timer UI composables exist but may not be fully wired

**Concerns:**
- shortService ~6min window may not suffice for long rest intervals (e.g., 10-20 min)
- Background: service may be killed on process death before timer completes; no explicit retry logic observed
- Notification: channel id 7777 — verify this doesn't conflict with other channels
- Cancellation: no explicit cancel action observed in composable; rely on system termination or back navigation
- App restart: timer state not persist across restarts (StateFlow in ViewModel, not in ViewModel + savedInstanceState)
- Process death: no surviving timer across app restarts

**Test/inspect needs:**
- Timer accuracy vs system clock
- Background execution on API 34+ (shortService limits)
- Notification channel behavior
- Cancellation on app background/foreground
- Process death and recovery
- Repeated timers (multiple workouts in session)