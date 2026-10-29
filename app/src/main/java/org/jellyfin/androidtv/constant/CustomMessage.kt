package org.jellyfin.androidtv.constant

sealed interface CustomMessage {
	data object ActionComplete : CustomMessage
}
