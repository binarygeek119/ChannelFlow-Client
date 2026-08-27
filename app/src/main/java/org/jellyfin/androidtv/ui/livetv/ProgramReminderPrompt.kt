package org.jellyfin.androidtv.ui.livetv

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.channelflow.ChannelFlowReminder
import org.jellyfin.androidtv.databinding.ProgramReminderPromptBinding

object ProgramReminderPrompt {
	private var dialog: Dialog? = null

	fun show(
		activity: FragmentActivity,
		reminder: ChannelFlowReminder,
		onWatchNow: (ChannelFlowReminder) -> Unit,
		onKeepWatching: (ChannelFlowReminder) -> Unit,
	) {
		dismiss()
		val binding = ProgramReminderPromptBinding.inflate(activity.layoutInflater)
		binding.title.text = reminder.title
		val episode = reminder.episodeTitle
		binding.episodeTitle.isVisible = !episode.isNullOrBlank()
		binding.episodeTitle.text = episode
		val channel = reminder.channelLabel
		binding.channel.isVisible = !channel.isNullOrBlank()
		binding.channel.text = channel
		binding.message.setText(R.string.msg_watch_reminder)

		val window = Dialog(activity, R.style.Theme_Jellyfin).apply {
			requestWindowFeature(Window.FEATURE_NO_TITLE)
			setCanceledOnTouchOutside(false)
			setCancelable(true)
			setOnDismissListener {
				if (dialog == this) dialog = null
			}
			setOnCancelListener { onKeepWatching(reminder) }
		}
		window.window?.decorView?.let { decor ->
			decor.setViewTreeLifecycleOwner(activity)
			decor.setViewTreeSavedStateRegistryOwner(activity)
		}
		window.setContentView(binding.root)
		window.window?.apply {
			setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			val metrics = activity.resources.displayMetrics
			setLayout((metrics.widthPixels * 0.72f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
			setDimAmount(0.78f)
		}

		binding.watchNowButton.setOnClickListener {
			dismiss()
			onWatchNow(reminder)
		}
		binding.keepWatchingButton.setOnClickListener {
			dismiss()
			onKeepWatching(reminder)
		}

		dialog = window
		window.setOnShowListener { binding.watchNowButton.requestFocus() }
		window.show()
	}

	fun isShowing(): Boolean = dialog?.isShowing == true

	fun dismiss() {
		dialog?.setOnCancelListener(null)
		dialog?.dismiss()
		dialog = null
	}
}
