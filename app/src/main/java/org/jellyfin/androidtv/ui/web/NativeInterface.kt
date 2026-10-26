package org.jellyfin.androidtv.ui.web

import android.content.Intent
import android.webkit.JavascriptInterface
import androidx.fragment.app.FragmentActivity
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("unused")
class NativeInterface(private val activity: FragmentActivity) : KoinComponent {
	private val api by inject<ApiClient>()

	@JavascriptInterface
	fun getDeviceInformation(): String = buildJsonObject {
		put("deviceId", api.deviceInfo.id)
		put("deviceName", AuthorizationHeaderBuilder.encodeParameterValue(api.deviceInfo.name))
		put("appName", api.clientInfo.name)
		put("appVersion", api.clientInfo.version)
	}.toString()

	@JavascriptInterface
	fun openClientSettings() = activity.runOnUiThread {
		activity.getViewModel<SettingsViewModel>().show()
	}

	@JavascriptInterface
	fun openServerSelection() = activity.runOnUiThread {
		activity.startActivity(Intent(activity, StartupActivity::class.java).putExtra(StartupActivity.EXTRA_SELECT_SERVER, true))
		activity.finish()
	}

	@JavascriptInterface
	fun exitApp() = activity.runOnUiThread {
		activity.finish()
	}
}
