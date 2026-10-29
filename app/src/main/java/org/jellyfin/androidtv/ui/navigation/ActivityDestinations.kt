package org.jellyfin.androidtv.ui.navigation

import android.content.Context
import android.content.Intent
import org.jellyfin.androidtv.ui.playback.ExternalPlayerActivity
import org.jellyfin.androidtv.util.createBundle
import kotlin.time.Duration

object ActivityDestinations {
	fun externalPlayer(context: Context, position: Duration = Duration.ZERO) = Intent(context, ExternalPlayerActivity::class.java).apply {
		putExtras(
			createBundle {
				putLong(ExternalPlayerActivity.EXTRA_POSITION, position.inWholeMilliseconds)
			}
		)
	}
}
