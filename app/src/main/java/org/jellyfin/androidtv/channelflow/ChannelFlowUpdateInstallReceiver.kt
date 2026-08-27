package org.jellyfin.androidtv.channelflow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ChannelFlowUpdateInstallReceiver : BroadcastReceiver(), KoinComponent {
	private val updater by inject<ChannelFlowUpdateChecker>()

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != ChannelFlowUpdateChecker.ACTION_INSTALL_STATUS) return
		val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
		val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
		val latest = intent.getStringExtra(ChannelFlowUpdateChecker.EXTRA_VERSION).orEmpty()
		when (status) {
			PackageInstaller.STATUS_PENDING_USER_ACTION -> {
				val confirm = confirmationIntent(intent)
				if (confirm != null) {
					confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
					context.startActivity(confirm)
					if (latest.isNotBlank()) updater.onInstallCommitted(latest)
				} else {
					updater.onInstallFailed(message)
				}
			}
			PackageInstaller.STATUS_SUCCESS -> updater.onInstallSucceeded()
			PackageInstaller.STATUS_FAILURE_ABORTED -> updater.restorePending()
			else -> {
				Timber.w("ChannelFlow update install failed status=%s %s", status, message)
				updater.onInstallFailed(message)
			}
		}
	}

	private fun confirmationIntent(intent: Intent): Intent? =
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
		} else {
			@Suppress("DEPRECATION")
			intent.getParcelableExtra(Intent.EXTRA_INTENT)
		}
}
