package org.jellyfin.androidtv.data.repository

import org.jellyfin.sdk.model.api.CollectionType

interface UserViewsRepository {
	fun isSupported(collectionType: CollectionType?): Boolean
}

class UserViewsRepositoryImpl : UserViewsRepository {
	override fun isSupported(collectionType: CollectionType?) = collectionType !in unsupportedCollectionTypes

	private companion object {
		private val unsupportedCollectionTypes = arrayOf(
			CollectionType.BOOKS,
			CollectionType.FOLDERS
		)
	}
}
