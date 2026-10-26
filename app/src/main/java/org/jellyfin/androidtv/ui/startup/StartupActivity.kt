package org.jellyfin.androidtv.ui.startup

import android.Manifest
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.jellyfin.androidtv.databinding.ActivityStartupBinding
import org.jellyfin.androidtv.integration.LeanbackChannelWorker
import org.jellyfin.androidtv.ui.background.AppBackground
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.ui.startup.fragment.SelectServerFragment
import org.jellyfin.androidtv.ui.startup.fragment.StartupToolbarFragment
import org.jellyfin.androidtv.util.applyTheme
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.net.URLEncoder
import java.util.UUID

/**
 * Map a launch intent's action, search query and item id to a jellyfin-web route.
 */
fun getWebRoute(action: String?, query: String?, itemId: String?): String? {
	val id = itemId?.toUUIDOrNull()
	return when {
		action == Intent.ACTION_SEARCH -> "/search?query=${URLEncoder.encode(query.orEmpty(), "UTF-8")}"
		id != null -> "/details?id=${id.toString().replace("-", "")}"
		else -> null
	}
}

class StartupActivity : FragmentActivity() {
	companion object {
		const val EXTRA_ITEM_ID = "ItemId"
		const val EXTRA_HIDE_SPLASH = "HideSplash"
		const val EXTRA_SELECT_SERVER = "SelectServer"
	}

	private val authenticationPreferences: AuthenticationPreferences by inject()
	private val authenticationStore: AuthenticationStore by inject()
	private val workManager: WorkManager by inject()

	private val networkPermissionsRequester = registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { grants ->
		val anyRejected = grants.any { !it.value }

		if (anyRejected) {
			// Permission denied, exit the app.
			Toast.makeText(this, R.string.no_network_permissions, Toast.LENGTH_LONG).show()
			finish()
		} else {
			onPermissionsGranted()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		applyTheme()

		super.onCreate(savedInstanceState)

		val binding = ActivityStartupBinding.inflate(layoutInflater)
		binding.background.setContent { AppBackground() }
		setContentView(binding.root)

		// Ensure basic permissions
		networkPermissionsRequester.launch(arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE))
	}

	override fun onResume() {
		super.onResume()

		applyTheme()
	}

	private fun onPermissionsGranted() {
		val serverId = authenticationPreferences[AuthenticationPreferences.lastServerId].toUUIDOrNull()

		if (serverId != null && authenticationStore.getServer(serverId) != null && !intent.getBooleanExtra(EXTRA_SELECT_SERVER, false)) {
			val itemId = intent.dataString ?: intent.getStringExtra(EXTRA_ITEM_ID)
			openMainActivity(serverId, getWebRoute(intent.action, intent.getStringExtra(SearchManager.QUERY), itemId))
		} else {
			supportFragmentManager.commit {
				replace<StartupToolbarFragment>(R.id.content_view)
				add<SelectServerFragment>(R.id.content_view)
			}
		}
	}

	fun openMainActivity(serverId: UUID, route: String? = null) {
		authenticationPreferences[AuthenticationPreferences.lastServerId] = serverId.toString()
		ProcessLifecycleOwner.get().lifecycleScope.launch { LeanbackChannelWorker.enqueue(workManager) }

		val intent = Intent(this, MainActivity::class.java)
			.putExtra(MainActivity.EXTRA_WEB_ROUTE, route)
			// Clear navigation history
			.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
		Timber.i("Opening server $serverId with route $route")
		startActivity(intent)
		finishAfterTransition()
	}
}
