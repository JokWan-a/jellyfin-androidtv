package org.jellyfin.androidtv.data.eventhandling

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.sockets.subscribe
import org.jellyfin.sdk.model.api.PlaystateCommand
import org.jellyfin.sdk.model.api.PlaystateMessage
import org.jellyfin.sdk.model.extensions.ticks
import timber.log.Timber

class SocketHandler(
	private val api: ApiClient,
	private val playbackControllerContainer: PlaybackControllerContainer,
	private val lifecycle: Lifecycle,
) {
	init {
		lifecycle.coroutineScope.launch(Dispatchers.IO) {
			lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
				subscribe(this)
			}
		}
	}

	private fun subscribe(coroutineScope: CoroutineScope) = api.webSocket.apply {
		subscribe<PlaystateMessage>()
			.onEach { message -> onPlayStateMessage(message) }
			.launchIn(coroutineScope)
	}

	@Suppress("ComplexMethod")
	private suspend fun onPlayStateMessage(message: PlaystateMessage) = withContext(Dispatchers.Main) {
		Timber.i("Received PlayStateMessage with command ${message.data?.command}")

		val playbackController = playbackControllerContainer.playbackController
		when (message.data?.command) {
			PlaystateCommand.STOP -> playbackController?.endPlayback(true)
			PlaystateCommand.PAUSE, PlaystateCommand.UNPAUSE, PlaystateCommand.PLAY_PAUSE -> playbackController?.playPause()
			PlaystateCommand.NEXT_TRACK -> playbackController?.next()
			PlaystateCommand.PREVIOUS_TRACK -> playbackController?.prev()
			PlaystateCommand.SEEK -> playbackController?.seek(
				message.data?.seekPositionTicks?.ticks?.inWholeMilliseconds ?: 0
			)

			PlaystateCommand.REWIND -> playbackController?.rewind()
			PlaystateCommand.FAST_FORWARD -> playbackController?.fastForward()

			null -> Unit
		}
	}
}
