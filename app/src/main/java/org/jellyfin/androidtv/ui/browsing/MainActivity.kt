package org.jellyfin.androidtv.ui.browsing

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.data.eventhandling.SocketHandler
import org.jellyfin.androidtv.integration.LeanbackChannelWorker
import org.jellyfin.androidtv.ui.InteractionTrackerViewModel
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.ProvideLocalInteractionTracker
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.composable.compat.AppNavigationHost
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.settings.compat.MainActivitySettings
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.ui.web.JellyfinWebViewClient
import org.jellyfin.androidtv.ui.web.NativeInterface
import org.jellyfin.androidtv.ui.web.NativePlayer
import org.jellyfin.androidtv.util.applyTheme
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : FragmentActivity() {
	companion object {
		const val EXTRA_WEB_ROUTE = "WebRoute"
		private const val MINIMUM_WEB_VIEW_VERSION = 80
	}

	private val navigationRepository by inject<NavigationRepository>()
	private val serverRepository by inject<ServerRepository>()
	private val authenticationPreferences by inject<AuthenticationPreferences>()
	private val interactionTrackerViewModel by viewModel<InteractionTrackerViewModel>()
	private val workManager by inject<WorkManager>()

	private val nativeInterface = NativeInterface(this)
	private lateinit var webView: WebView

	// Set once the web app requested its main bundle, from a WebView background thread
	@Volatile
	private var connected = false
	private var error by mutableStateOf<String?>(null)

	private val backCallback = object : OnBackPressedCallback(true) {
		override fun handleOnBackPressed() {
			if (connected) webView.evaluateJavascript("window.NavigationHelper.goBack()", null)
			else finish()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		applyTheme()

		super.onCreate(savedInstanceState)

		// Added before any other callback so it has the lowest priority
		onBackPressedDispatcher.addCallback(this, backCallback)

		interactionTrackerViewModel.keepScreenOn.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
			.onEach { keepScreenOn ->
				if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
				else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			}.launchIn(lifecycleScope)

		if (savedInstanceState == null) navigationRepository.reset(clearHistory = true)

		// Start handling remote playstate commands for the legacy player
		get<SocketHandler>()

		webView = WebView(this)

		setContent {
			// The native player is shown on top of the web app, it's active when there is navigation history
			val playerActive by remember {
				navigationRepository.currentAction.map { navigationRepository.canGoBack }.distinctUntilChanged()
			}.collectAsState(navigationRepository.canGoBack)
			LaunchedEffect(playerActive) { onPlayerActiveChanged(playerActive) }

			JellyfinTheme {
				ProvideLocalInteractionTracker(
					interactionTracker = { interactionTrackerViewModel.notifyInteraction(userInitiated = true) }
				) {
					Box {
						// Keep the web app on top of the (transparent) navigation host while it's in use so it receives pointer input
						AndroidView(
							factory = { webView },
							modifier = Modifier
								.fillMaxSize()
								.zIndex(if (playerActive) 0f else 1f),
						)
						AppNavigationHost(
							navigationRepository = navigationRepository,
						)
					}
					MainActivitySettings()
					error?.let { message ->
						WebErrorOverlay(
							message = message,
							onRetry = ::recreate,
							onChangeServer = nativeInterface::openServerSelection,
						)
					}
				}
			}
		}

		lifecycleScope.launch {
			val serverId = authenticationPreferences[AuthenticationPreferences.lastServerId].toUUIDOrNull()
			val server = serverId?.let { serverRepository.getServer(it) }
			if (server == null) {
				startActivity(Intent(this@MainActivity, StartupActivity::class.java))
				finish()
			} else {
				loadWebApp(server)
			}
		}
	}

	private fun onPlayerActiveChanged(playerActive: Boolean) {
		backCallback.isEnabled = !playerActive
		webView.isFocusable = !playerActive
		webView.isFocusableInTouchMode = !playerActive
		if (playerActive) {
			webView.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
			webView.onPause()
		} else {
			webView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
			webView.onResume()
			webView.requestFocus()
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	private fun loadWebApp(server: Server) {
		val webViewVersion = WebViewCompat.getCurrentWebViewPackage(this)?.versionName?.substringBefore('.')?.toIntOrNull() ?: 0
		if (webViewVersion < MINIMUM_WEB_VIEW_VERSION) {
			showError(getString(R.string.web_view_outdated))
			return
		}

		webView.webViewClient = JellyfinWebViewClient(
			server = server,
			assetsPathHandler = WebViewAssetLoader.AssetsPathHandler(this),
			coroutineScope = lifecycleScope,
			onConnected = { connected = true },
			onError = {
				connected = false
				showError(getString(R.string.server_connection_failed))
			},
		)
		webView.settings.apply {
			javaScriptEnabled = true
			domStorageEnabled = true
			mediaPlaybackRequiresUserGesture = false
			userAgentString = "$userAgentString JellyfinAndroidTV/${BuildConfig.VERSION_NAME}"
		}
		webView.addJavascriptInterface(nativeInterface, "NativeInterface")
		webView.addJavascriptInterface(NativePlayer(this), "NativePlayer")

		val route = intent.getStringExtra(EXTRA_WEB_ROUTE)?.let { "#$it" }.orEmpty()
		webView.loadUrl("${server.address.trimEnd('/')}/web/index.html$route")
	}

	private fun showError(message: String) {
		error = message
		// Hidden so it can't take focus from the overlay
		webView.isInvisible = true
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)

		val route = intent.getStringExtra(EXTRA_WEB_ROUTE) ?: return
		if (connected) webView.evaluateJavascript("window.Emby.Page.show(${Json.encodeToString(route)})", null)
	}

	override fun onResume() {
		super.onResume()

		applyTheme()
	}

	override fun onStop() {
		super.onStop()

		workManager.enqueue(OneTimeWorkRequestBuilder<LeanbackChannelWorker>().build())
	}

	override fun onDestroy() {
		super.onDestroy()

		webView.destroy()
	}

	// Forward key events to fragments

	private fun Fragment.onKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
		var result = childFragmentManager.fragments.any { it.onKeyEvent(keyCode, event) }
		if (!result && this is View.OnKeyListener) result = onKey(currentFocus, keyCode, event)
		return result
	}

	private fun onKeyEvent(keyCode: Int, event: KeyEvent?): Boolean = supportFragmentManager.fragments
		.any { it.onKeyEvent(keyCode, event) }

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyDown(keyCode, event)

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyUp(keyCode, event)

	override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyUp(keyCode, event)
}

@Composable
private fun WebErrorOverlay(
	message: String,
	onRetry: () -> Unit,
	onChangeServer: () -> Unit,
) {
	val focusRequester = remember { FocusRequester() }

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(JellyfinTheme.colorScheme.background),
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(24.dp),
		) {
			Text(message, color = JellyfinTheme.colorScheme.onBackground)
			Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
				Button(onClick = onRetry, modifier = Modifier.focusRequester(focusRequester)) {
					Text(stringResource(R.string.action_connect))
				}
				Button(onClick = onChangeServer) {
					Text(stringResource(R.string.lbl_manage_servers))
				}
			}
		}
	}

	LaunchedEffect(focusRequester) {
		focusRequester.requestFocus()
	}
}
