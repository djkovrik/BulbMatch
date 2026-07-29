package com.sedsoftware.bulbmatch.data.settings

import com.sedsoftware.bulbmatch.data.history.SqlDelightSavedMatchStore

/**
 * Clears durable product data without resetting display choices.
 *
 * SQLDelight owns the transactional history deletion. Multiplatform Settings
 * writes are synchronous; the counter reset runs only after that transaction
 * commits. Locale and theme keys are deliberately untouched.
 */
class LocalDataCleaner<T : Any>(
    private val savedMatches: SqlDelightSavedMatchStore<T>,
    private val settings: BulbMatchSettingsStore,
) {
    suspend fun clear() {
        savedMatches.clearAll()
        settings.resetAdFrequency()
    }
}
