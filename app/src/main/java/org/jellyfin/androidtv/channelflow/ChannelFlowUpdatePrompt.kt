package org.jellyfin.androidtv.channelflow

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.UpdatePromptBinding

object ChannelFlowUpdatePrompt {
	private var dialog: Dialog? = null

	fun show(
		activity: FragmentActivity,
		available: ChannelFlowUpdateStatus.Available,
		onInstall: (ChannelFlowUpdateStatus.Available) -> Unit,
		onLater: (ChannelFlowUpdateStatus.Available) -> Unit,
	) {
		dismiss()
		val binding = UpdatePromptBinding.inflate(activity.layoutInflater)
		binding.title.text = ChannelFlowVersion.display(available.latest)
		binding.message.text = activity.getString(
			R.string.msg_update_ready,
			ChannelFlowVersion.display(available.latest),
			ChannelFlowVersion.display(available.installed),
		)

		val window = Dialog(activity, R.style.Theme_Jellyfin).apply {
			requestWindowFeature(Window.FEATURE_NO_TITLE)
			setCanceledOnTouchOutside(false)
			setCancelable(true)
			setOnDismissListener {
				if (dialog == this) dialog = null
			}
			setOnCancelListener { onLater(available) }
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

		binding.installButton.setOnClickListener {
			dismiss()
			onInstall(available)
		}
		binding.laterButton.setOnClickListener {
			dismiss()
			onLater(available)
		}

		dialog = window
		window.setOnShowListener { binding.installButton.requestFocus() }
		window.show()
	}

	fun isShowing(): Boolean = dialog?.isShowing == true

	fun dismiss() {
		dialog?.setOnCancelListener(null)
		dialog?.dismiss()
		dialog = null
	}
}
