package org.jellyfin.androidtv.preference

/**
 * Repository to access special preference stores.
 */
class PreferencesRepository(
	private val liveTvPreferences: LiveTvPreferences,
	private val userSettingPreferences: UserSettingPreferences,
) {
	suspend fun onSessionChanged() {
		// Note: Do not run parallel as the server can't deal with that
		// Relevant server issue: https://github.com/jellyfin/jellyfin/issues/5261
		liveTvPreferences.update()
		userSettingPreferences.update()
	}
}
