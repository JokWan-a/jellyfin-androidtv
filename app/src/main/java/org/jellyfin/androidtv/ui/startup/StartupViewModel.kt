package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jellyfin.androidtv.auth.repository.ServerRepository
import java.util.UUID

class StartupViewModel(
	private val serverRepository: ServerRepository,
) : ViewModel() {
	val storedServers = serverRepository.storedServers
	val discoveredServers = serverRepository.discoveredServers

	private val discoveryMutex = Mutex()

	fun addServer(address: String) = serverRepository.addServer(address)

	fun deleteServer(serverId: UUID) {
		viewModelScope.launch { serverRepository.deleteServer(serverId) }
	}

	fun loadDiscoveryServers() {
		// Only run one discovery process at a time
		if (discoveryMutex.isLocked) return

		viewModelScope.launch(Dispatchers.IO) {
			discoveryMutex.withLock {
				serverRepository.loadDiscoveryServers()
			}
		}
	}

	fun reloadStoredServers() {
		viewModelScope.launch { serverRepository.loadStoredServers() }
	}
}
