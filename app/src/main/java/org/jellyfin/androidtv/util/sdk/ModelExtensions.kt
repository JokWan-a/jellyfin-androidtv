@file:JvmName("ModelUtils")

package org.jellyfin.androidtv.util.sdk

import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.sdk.model.api.ServerDiscoveryInfo
import org.jellyfin.sdk.model.serializer.toUUID

fun ServerDiscoveryInfo.toServer(): Server = Server(
	id = id.toUUID(),
	name = name,
	address = address,
)
