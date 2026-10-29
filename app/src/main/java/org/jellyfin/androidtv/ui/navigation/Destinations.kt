package org.jellyfin.androidtv.ui.navigation

import org.jellyfin.androidtv.ui.playback.CustomPlaybackOverlayFragment
import org.jellyfin.androidtv.ui.playback.nextup.NextUpFragment
import org.jellyfin.androidtv.ui.playback.stillwatching.StillWatchingFragment
import org.jellyfin.androidtv.ui.player.video.VideoPlayerFragment
import org.jellyfin.androidtv.ui.web.BlankFragment
import java.util.UUID

object Destinations {
	// General
	val webApp = fragmentDestination<BlankFragment>()

	// Playback
	fun videoPlayer(position: Int?) = fragmentDestination<CustomPlaybackOverlayFragment> {
		putInt("Position", position ?: 0)
	}

	fun videoPlayerNew(position: Int?) = fragmentDestination<VideoPlayerFragment> {
		putInt(VideoPlayerFragment.EXTRA_POSITION, position ?: 0)
	}

	fun nextUp(item: UUID) = fragmentDestination<NextUpFragment> {
		putString(NextUpFragment.ARGUMENT_ITEM_ID, item.toString())
	}

	fun stillWatching(item: UUID) = fragmentDestination<StillWatchingFragment> {
		putString(NextUpFragment.ARGUMENT_ITEM_ID, item.toString())
	}
}
