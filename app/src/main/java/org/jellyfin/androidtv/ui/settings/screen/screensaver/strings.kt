package org.jellyfin.androidtv.ui.settings.screen.screensaver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R

@Composable
@Stable
fun getScreensaverAgeRatingOptions() = buildList {
	add(0 to stringResource(R.string.pref_screensaver_ageratingmax_zero))
	setOf(5, 10, 13, 14, 16, 18, 21)
		.forEach { age -> add(age to stringResource(R.string.pref_screensaver_ageratingmax_entry, age)) }
	add(-1 to stringResource(R.string.pref_screensaver_ageratingmax_unlimited))
}
