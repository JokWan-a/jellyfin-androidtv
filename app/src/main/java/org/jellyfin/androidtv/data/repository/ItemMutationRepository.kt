package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.UserItemDataDto

interface ItemMutationRepository {
	suspend fun setFavorite(item: UUID, favorite: Boolean): UserItemDataDto
}

class ItemMutationRepositoryImpl(
	private val api: ApiClient,
) : ItemMutationRepository {
	override suspend fun setFavorite(item: UUID, favorite: Boolean): UserItemDataDto {
		val response by when {
			favorite -> withContext(Dispatchers.IO) { api.userLibraryApi.markFavoriteItem(itemId = item) }
			else -> withContext(Dispatchers.IO) { api.userLibraryApi.unmarkFavoriteItem(itemId = item) }
		}

		return response
	}
}
