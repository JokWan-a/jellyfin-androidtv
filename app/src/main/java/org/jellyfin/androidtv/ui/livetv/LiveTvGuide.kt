package org.jellyfin.androidtv.ui.livetv

import android.content.Context
import android.widget.RelativeLayout
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import java.time.LocalDateTime
import java.util.UUID

interface LiveTvGuide {
	companion object {
		const val GUIDE_ROW_HEIGHT_DP = 55
		const val GUIDE_ROW_WIDTH_PER_MIN_DP = 7
		const val PAGE_SIZE = 75
	}

	fun displayChannels(start: Int, max: Int)
	fun getCurrentLocalStartDate(): LocalDateTime
	fun showProgramOptions()
	fun setSelectedProgram(programView: RelativeLayout)
	fun refreshFavorite(channelId: UUID)
}

fun createNoProgramDataBaseItem(
	context: Context,
	channelId: UUID?,
	startDate: LocalDateTime?,
	endDate: LocalDateTime?
) = BaseItemDto(
	id = UUID.randomUUID(),
	type = BaseItemKind.FOLDER,
	mediaType = MediaType.UNKNOWN,
	name = context.getString(R.string.no_program_data),
	channelId = channelId,
	startDate = startDate,
	endDate = endDate,
)
