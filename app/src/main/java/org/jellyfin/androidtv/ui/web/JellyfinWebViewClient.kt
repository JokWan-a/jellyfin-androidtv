package org.jellyfin.androidtv.ui.web

import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jellyfin.androidtv.auth.model.AuthenticatedState
import org.jellyfin.androidtv.auth.model.AuthenticatingState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val MAIN_BUNDLE_PATH_REGEX = Regex(""".*/main\.[^/\s]+\.bundle\.js""")
private const val SESSION_CAPABILITIES_PATH = "sessions/capabilities/full"
private const val SESSION_LOGOUT_PATH = "sessions/logout"

/**
 * Read the access token from the result of `localStorage.getItem('jellyfin_credentials')`. The
 * evaluateJavascript result is a JSON encoded string containing the credentials JSON.
 */
internal fun parseWebCredentials(result: String): String? = try {
	Json.decodeFromString<String?>(result)?.let { credentials ->
		Json.parseToJsonElement(credentials).jsonObject["Servers"]?.jsonArray?.firstOrNull()
			?.jsonObject?.get("AccessToken")?.jsonPrimitive?.contentOrNull
	}
} catch (err: IllegalArgumentException) {
	Timber.w(err, "Unable to parse web credentials")
	null
}

class JellyfinWebViewClient(
	private val server: Server,
	private val assetsPathHandler: WebViewAssetLoader.AssetsPathHandler,
	private val coroutineScope: CoroutineScope,
	private val onConnected: () -> Unit,
	private val onError: () -> Unit,
) : WebViewClientCompat(), KoinComponent {
	private val authenticationRepository by inject<AuthenticationRepository>()
	private val sessionRepository by inject<SessionRepository>()
	private val serverOrigin = server.address.toUri().origin

	override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
		val url = request.url
		val path = url.path?.lowercase(Locale.ROOT) ?: return null
		return when {
			path.matches(MAIN_BUNDLE_PATH_REGEX) && "deferred" !in url.query.orEmpty() -> {
				onConnected()
				assetsPathHandler.handle("native/injectionScript.js")
			}

			path.contains("/native/") -> assetsPathHandler.handle("native/${url.lastPathSegment}")

			path.endsWith(SESSION_CAPABILITIES_PATH) -> {
				coroutineScope.launch {
					val result = suspendCoroutine { continuation ->
						view.evaluateJavascript("localStorage.getItem('jellyfin_credentials')") { continuation.resume(it) }
					}
					val token = parseWebCredentials(result)
					if (token == null) {
						Timber.e("Unable to find access token in web credentials")
						return@launch
					}

					authenticationRepository.authenticateToken(server, token).collect { state ->
						if (state != AuthenticatingState && state != AuthenticatedState) Timber.e("Unable to adopt web session: $state")
					}
				}
				null
			}

			path.endsWith(SESSION_LOGOUT_PATH) -> {
				coroutineScope.launch {
					sessionRepository.currentSession.value?.let { authenticationRepository.logout(it.serverId, it.userId) }
					sessionRepository.destroyCurrentSession()
				}
				null
			}

			else -> null
		}
	}

	override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
		request.url.origin != serverOrigin

	override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
		Timber.e("Received WebView HTTP ${errorResponse.statusCode} error at ${request.url}")
		if (request.isForMainFrame) onError()
	}

	override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
		val description = if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION)) error.description else null
		Timber.e("Received WebView error at ${request.url}: $description")
		if (request.isForMainFrame) onError()
	}

	override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
		Timber.e("Received SSL error: $error")
		handler.cancel()
	}

	private val Uri.origin: String
		@Suppress("MagicNumber")
		get() {
			val scheme = scheme?.lowercase(Locale.ROOT)
			val port = if (port != -1) port else if (scheme == "https") 443 else 80
			return "$scheme://${host?.lowercase(Locale.ROOT)}:$port"
		}
}
