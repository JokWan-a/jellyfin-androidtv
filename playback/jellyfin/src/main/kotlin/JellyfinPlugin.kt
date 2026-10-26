package org.jellyfin.playback.jellyfin

import org.jellyfin.playback.core.plugin.playbackPlugin
import org.jellyfin.playback.jellyfin.lyrics.LyricsPlayerService
import org.jellyfin.playback.jellyfin.mediasegment.MediaSegmentService
import org.jellyfin.playback.jellyfin.mediastream.JellyfinMediaStreamResolver
import org.jellyfin.playback.jellyfin.playsession.PlaySessionService
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.MediaSegmentType

fun jellyfinPlugin(
	api: ApiClient,
	deviceProfileBuilder: () -> DeviceProfile,
	mediaSegmentSkipTypes: Set<MediaSegmentType> = emptySet(),
) = playbackPlugin {
	provide(JellyfinMediaStreamResolver(api, deviceProfileBuilder))
	provide(PlaySessionService(api))
	provide(LyricsPlayerService(api))

	if (mediaSegmentSkipTypes.isNotEmpty()) provide(MediaSegmentService(api, mediaSegmentSkipTypes))
}
