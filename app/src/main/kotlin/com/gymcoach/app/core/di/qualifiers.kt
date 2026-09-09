package com.gymcoach.app.core.di

import javax.inject.Qualifier

/**
 * Qualifier for the application-scoped CoroutineScope.
 *
 * This scope survives ViewModel destruction and Activity lifecycle events.
 * Use it for critical persistence operations that MUST complete even if the
 * user navigates away (e.g., completing a workout).
 *
 * Do NOT use this for UI updates, state flow emissions, or operations that
 * should be cancelled when the ViewModel is cleared.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
