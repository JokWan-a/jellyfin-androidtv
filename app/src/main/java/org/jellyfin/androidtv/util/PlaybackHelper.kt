package org.jellyfin.androidtv.util

import android.content.Context
import java.util.UUID

interface PlaybackHelper {
	fun retrieveAndPlay(itemIds: List<UUID>, shuffle: Boolean, position: Long?, index: Int?, context: Context)
}
