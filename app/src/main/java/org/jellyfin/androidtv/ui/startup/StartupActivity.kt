package org.jellyfin.androidtv.ui.startup

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.SessionRepositoryState
import org.jellyfin.androidtv.channelflow.ChannelFlowConnectionStore
import org.jellyfin.androidtv.databinding.ActivityStartupBinding
import org.jellyfin.androidtv.ui.background.AppBackground
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.startup.fragment.ConnectPinFragment
import org.jellyfin.androidtv.ui.startup.fragment.SplashFragment
import org.jellyfin.androidtv.util.applyTheme
import org.koin.android.ext.android.inject

class StartupActivity : FragmentActivity() {
	companion object {
		const val EXTRA_HIDE_SPLASH = "HideSplash"
		const val EXTRA_RECONNECT = "Reconnect"
	}

	private val sessionRepository: SessionRepository by inject()
	private val navigationRepository: NavigationRepository by inject()
	private val connectionStore: ChannelFlowConnectionStore by inject()

	private lateinit var binding: ActivityStartupBinding

	private val networkPermissionsRequester = registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { grants ->
		val anyRejected = grants.any { !it.value }

		if (anyRejected) {
			Toast.makeText(this, R.string.no_network_permissions, Toast.LENGTH_LONG).show()
			finish()
		} else {
			onPermissionsGranted()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		applyTheme()

		super.onCreate(savedInstanceState)

		binding = ActivityStartupBinding.inflate(layoutInflater)
		binding.background.setContent { AppBackground() }
		binding.screensaver.isVisible = false
		setContentView(binding.root)

		if (!intent.getBooleanExtra(EXTRA_HIDE_SPLASH, false)) showSplash()

		networkPermissionsRequester.launch(arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE))
	}

	override fun onResume() {
		super.onResume()

		applyTheme()
	}

	private fun onPermissionsGranted() = sessionRepository.state
		.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
		.filter { it == SessionRepositoryState.READY }
		.take(1)
		.onEach {
			continueAfterSplash()
		}.launchIn(lifecycleScope)

	private fun continueAfterSplash() {
		if (intent.getBooleanExtra(EXTRA_RECONNECT, false) || !connectionStore.isConnected) {
			showConnectPin()
		} else {
			openMain()
		}
	}

	fun showConnectPin() {
		supportFragmentManager.commit {
			replace(R.id.content_view, ConnectPinFragment())
		}
	}

	fun openMain() {
		navigationRepository.reset(Destinations.liveTvGuide, true)

		val intent = Intent(this, MainActivity::class.java)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
		startActivity(intent)
		finishAfterTransition()
	}

	private fun showSplash() {
		if (supportFragmentManager.findFragmentById(R.id.content_view) is SplashFragment) return

		supportFragmentManager.commit {
			replace<SplashFragment>(R.id.content_view)
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
	}
}
