package org.jellyfin.androidtv.auth.repository

import org.jellyfin.androidtv.auth.model.PrivateUser
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.store.AuthenticationStore

/**
 * Repository to maintain users for servers.
 * Authentication is done using the [AuthenticationRepository].
 */
interface ServerUserRepository {
	//server
	fun getStoredServerUsers(server: Server): List<PrivateUser>

	fun deleteStoredUser(user: PrivateUser)
}

class ServerUserRepositoryImpl(
	private val authenticationStore: AuthenticationStore,
) : ServerUserRepository {
	override fun getStoredServerUsers(server: Server) = authenticationStore.getUsers(server.id)
		?.mapNotNull { (userId, userInfo) ->
			val authInfo = authenticationStore.getUser(server.id, userId)
			PrivateUser(
				id = userId,
				serverId = server.id,
				name = userInfo.name,
				accessToken = authInfo?.accessToken,
				imageTag = userInfo.imageTag,
				lastUsed = userInfo.lastUsed,
			)
		}
		?.sortedWith(compareByDescending<PrivateUser> { it.lastUsed }.thenBy { it.name })
		.orEmpty()

	override fun deleteStoredUser(user: PrivateUser) {
		// Remove user info from store
		authenticationStore.removeUser(user.serverId, user.id)
	}
}
