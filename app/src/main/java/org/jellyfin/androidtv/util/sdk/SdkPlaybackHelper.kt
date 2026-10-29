package org.jellyfin.androidtv.util.sdk

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.androidtv.util.PlaybackHelper
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.extensions.ticks
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SdkPlaybackHelper(
	private val api: ApiClient,
	private val userPreferences: UserPreferences,
	private val playbackLauncher: PlaybackLauncher,
	private val playbackControllerContainer: PlaybackControllerContainer,
) : PlaybackHelper {
	override fun retrieveAndPlay(itemIds: List<UUID>, shuffle: Boolean, position: Long?, index: Int?, context: Context) {
		getScope(context).launch {
			val resumeSubtractDuration =
				userPreferences[UserPreferences.resumeSubtractDuration].toIntOrNull()?.seconds
					?: Duration.ZERO

			val items = withContext(Dispatchers.IO) {
				val response by api.itemsApi.getItems(
					ids = itemIds,
				)
				response.items
			}

			val pos = position?.ticks ?: items[0].userData?.playbackPositionTicks?.ticks?.minus(
				resumeSubtractDuration
			) ?: Duration.ZERO

			playbackLauncher.launch(
				context,
				items,
				pos.inWholeMilliseconds.toInt(),
				playbackControllerContainer.playbackController?.hasFragment() == true,
				index ?: 0,
				shuffle,
			)
		}
	}

	private fun getScope(context: Context? = null) = when (context) {
		is LifecycleOwner -> context.lifecycleScope
		else -> ProcessLifecycleOwner.get().lifecycleScope
	}
}
