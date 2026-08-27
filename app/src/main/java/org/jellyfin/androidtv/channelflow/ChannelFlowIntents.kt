package org.jellyfin.androidtv.channelflow

import android.content.Context
import android.content.Intent
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.ui.startup.StartupActivity

fun Context.startChannelFlowPairing() {
	startActivity(
		Intent(this, StartupActivity::class.java).apply {
			putExtra(StartupActivity.EXTRA_RECONNECT, true)
			putExtra(StartupActivity.EXTRA_HIDE_SPLASH, true)
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
	)
}

fun Context.reloadChannelFlowMain() {
	startActivity(
		Intent(this, MainActivity::class.java).apply {
			addFlags(
				Intent.FLAG_ACTIVITY_NEW_TASK or
					Intent.FLAG_ACTIVITY_CLEAR_TASK or
					Intent.FLAG_ACTIVITY_TASK_ON_HOME
			)
		}
	)
}

fun Context.startChannelFlowPairingFromEmpty() {
	startActivity(
		Intent(this, StartupActivity::class.java).apply {
			addFlags(
				Intent.FLAG_ACTIVITY_NEW_TASK or
					Intent.FLAG_ACTIVITY_CLEAR_TASK or
					Intent.FLAG_ACTIVITY_TASK_ON_HOME
			)
		}
	)
}
