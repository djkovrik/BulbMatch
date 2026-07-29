package com.sedsoftware.bulbmatch.platform

enum class ScreenCode {
    MATCH_HOME,
    CAMERA_CAPTURE,
    IMAGE_REVIEW,
    FIELD_REVIEW,
    RESULT,
    HISTORY,
    SAVED_RESULT,
    REFERENCE,
    BASE_DETAIL,
    SETTINGS,
    ABOUT,
}

enum class OperationCode {
    CAMERA_PERMISSION_CHECK,
    CAMERA_PERMISSION_REQUEST,
    CAMERA_BIND,
    CAMERA_CAPTURE,
    IMAGE_PICK,
    IMAGE_DECODE,
    TEXT_RECOGNITION,
    DATABASE_READ,
    DATABASE_WRITE,
    AD_LOAD,
    AD_SHOW,
    APP_START,
}

/**
 * All report metadata is closed over enums. Arbitrary strings, OCR values, image
 * identifiers, names, and navigation breadcrumbs cannot cross this boundary.
 */
data class CrashContext(
    val screen: ScreenCode?,
    val operation: OperationCode,
)

interface CrashReporter {
    fun recordNonFatal(throwable: Throwable, context: CrashContext)
}

data object DisabledCrashReporter : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, context: CrashContext) = Unit
}
