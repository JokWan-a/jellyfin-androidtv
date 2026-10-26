package org.jellyfin.androidtv.ui.web

import android.webkit.JavascriptInterface
import androidx.fragment.app.FragmentActivity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.util.PlaybackHelper
import org.jellyfin.sdk.model.serializer.UUIDSerializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.UUID

@Serializable
data class LoadPlayerOptions(
	val ids: List<@Serializable(with = UUIDSerializer::class) UUID>,
	val startIndex: Int? = null,
	val startPositionTicks: Long? = null,
)

private val json = Json { ignoreUnknownKeys = true }

internal fun parseLoadPlayerOptions(options: String): LoadPlayerOptions? = try {
	json.decodeFromString<LoadPlayerOptions>(options)
} catch (err: IllegalArgumentException) {
	Timber.w(err, "Unable to parse player options")
	null
}

@Suppress("unused")
class NativePlayer(private val activity: FragmentActivity) : KoinComponent {
	private val playbackHelper by inject<PlaybackHelper>()

	@JavascriptInterface
	fun loadPlayer(options: String) {
		val playOptions = parseLoadPlayerOptions(options) ?: return
		activity.runOnUiThread {
			playbackHelper.retrieveAndPlay(playOptions.ids, false, playOptions.startPositionTicks, playOptions.startIndex, activity)
		}
	}
}
