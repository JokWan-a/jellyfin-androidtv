@file:JvmName("JavaCompat")

package org.jellyfin.androidtv.util.sdk.compat

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSourceInfo

fun BaseItemDto.copyWithTimerId(
	seriesTimerId: String?,
) = copy(
	seriesTimerId = seriesTimerId,
)

fun MediaSourceInfo.getVideoStream() = mediaStreams?.firstOrNull {
	it.type == org.jellyfin.sdk.model.api.MediaStreamType.VIDEO
}
