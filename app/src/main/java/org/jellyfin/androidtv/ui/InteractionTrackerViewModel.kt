package org.jellyfin.androidtv.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class InteractionTrackerViewModel(
	private val userPreferences: UserPreferences,
	private val playbackControllerContainer: PlaybackControllerContainer
) : ViewModel() {
	// Screensaver

	private var locks = 0

	// Still Watching

	private var isWatchingEpisodes = false
	private var episodeCount = 0
	private var watchTime = 0L
	private var episodeWasInterrupted: Boolean = false

	// Preferences

	private val stillWatchingBehavior get() = userPreferences[UserPreferences.stillWatchingBehavior]

	// State

	private val _keepScreenOn = MutableStateFlow(false)
	val keepScreenOn get() = _keepScreenOn.asStateFlow()

	fun getShowStillWatching(): Boolean {
		if (!stillWatchingBehavior.enabled) return false

		if (stillWatchingBehavior.episodeCount > 0 && episodeCount >= stillWatchingBehavior.episodeCount) return true
		if (stillWatchingBehavior.minDuration > Duration.ZERO && watchTime.milliseconds >= stillWatchingBehavior.minDuration) return true

		return false
	}

	fun notifyStartSession(item: BaseItemDto, items: List<BaseItemDto>) {
		// No need to track when only watching 1 episode
		if (item.type == BaseItemKind.EPISODE && items.size > 1) {
			resetSession()
			episodeWasInterrupted = false
		}
	}

	fun notifyStillWatching() {
		resetSession()
	}

	fun onEpisodeWatched() {
		if (!episodeWasInterrupted) episodeCount++
		watchTime += playbackControllerContainer.playbackController?.duration ?: 0
		episodeWasInterrupted = false
	}

	fun notifyStart(item: BaseItemDto) {
		if (item.type == BaseItemKind.EPISODE) isWatchingEpisodes = true
	}

	fun notifyInteraction(userInitiated: Boolean) {
		// If watching episodes, reset episode count and watch time
		if (isWatchingEpisodes && userInitiated) {
			resetSession()
			episodeWasInterrupted = true
		}

		// Update KEEP_SCREEN_ON flag value
		_keepScreenOn.value = locks > 0
	}

	/**
	 * Create a lock that prevents the screensaver for running until the returned function is called
	 * or the lifecycle is destroyed.
	 *
	 * @return Function to cancel the lock
	 */
	fun addLifecycleLock(lifecycle: Lifecycle): () -> Unit {
		if (lifecycle.currentState == Lifecycle.State.DESTROYED) return {}

		val lock = ScreensaverLock(lifecycle)
		lock.activate()
		return lock::cancel
	}

	private inner class ScreensaverLock(private val lifecycle: Lifecycle) : LifecycleEventObserver {
		private var active: Boolean = false

		override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
			if (event == Lifecycle.Event.ON_DESTROY) cancel()
		}

		fun activate() {
			if (active) return
			lifecycle.addObserver(this)
			locks++
			notifyInteraction(userInitiated = false)
			active = true
		}

		fun cancel() {
			if (!active) return
			locks--
			lifecycle.removeObserver(this)
			notifyInteraction(userInitiated = false)
			active = false
		}
	}

	private fun resetSession() {
		watchTime = 0L
		episodeCount = 0
	}
}
