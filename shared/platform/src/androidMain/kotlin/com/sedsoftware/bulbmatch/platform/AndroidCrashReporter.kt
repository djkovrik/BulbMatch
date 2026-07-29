package com.sedsoftware.bulbmatch.platform

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Native Crashlytics boundary. Creation is explicitly gated by the host so
 * debug/preview/test builds never send. If Firebase configuration is absent the
 * factory returns [DisabledCrashReporter].
 */
class AndroidCrashReporter private constructor(
    private val crashlytics: FirebaseCrashlytics,
) : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, context: CrashContext) {
        crashlytics.setCustomKey("screen_code", context.screen?.name ?: "NONE")
        crashlytics.setCustomKey("operation_code", context.operation.name)
        crashlytics.recordException(SanitizedNonFatal(throwable))
    }

    private class SanitizedNonFatal(original: Throwable) :
        RuntimeException(sanitizeType(original)) {
        init {
            stackTrace = original.stackTrace
        }

        companion object {
            private fun sanitizeType(original: Throwable): String =
                (original::class.qualifiedName ?: "UnknownThrowable")
                    .filter { it.isLetterOrDigit() || it == '.' || it == '_' }
                    .take(160)
        }
    }

    companion object {
        fun create(
            context: Context,
            collectionEnabled: Boolean,
        ): CrashReporter {
            if (!collectionEnabled) return DisabledCrashReporter
            val hasDefaultApp = FirebaseApp.getApps(context.applicationContext)
                .any { it.name == FirebaseApp.DEFAULT_APP_NAME }
            if (!hasDefaultApp) return DisabledCrashReporter
            return runCatching {
                FirebaseCrashlytics.getInstance().also {
                    it.setCrashlyticsCollectionEnabled(true)
                }
            }.fold(
                onSuccess = ::AndroidCrashReporter,
                onFailure = { DisabledCrashReporter },
            )
        }
    }
}
