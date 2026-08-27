package org.jellyfin.androidtv.ui.livetv

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.channelflow.ChannelFlowGuideChrome
import org.jellyfin.androidtv.channelflow.ChannelFlowGuideRepository
import org.jellyfin.androidtv.channelflow.ChannelFlowReminderScheduler
import org.jellyfin.androidtv.databinding.ProgramDetailDialogBinding
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.getTimeFormatter
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.android.ext.android.get
import java.time.Duration
import java.time.LocalDateTime

object ProgramDetailDialog {
	private var dialog: Dialog? = null

	fun show(fragment: Fragment, program: BaseItemDto) {
		val context = fragment.context ?: return
		dismiss()

		val binding = ProgramDetailDialogBinding.inflate(fragment.layoutInflater)
		bind(fragment, binding, program)

		val window = Dialog(context, R.style.Theme_Jellyfin).apply {
			requestWindowFeature(Window.FEATURE_NO_TITLE)
			setCanceledOnTouchOutside(true)
			setOnDismissListener { if (dialog == this) dialog = null }
		}
		window.window?.decorView?.let { decor ->
			decor.setViewTreeLifecycleOwner(fragment.viewLifecycleOwner)
			decor.setViewTreeSavedStateRegistryOwner(fragment)
		}
		window.setContentView(binding.root)
		window.window?.apply {
			setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			val metrics = context.resources.displayMetrics
			setLayout((metrics.widthPixels * 0.82f).toInt(), (metrics.heightPixels * 0.78f).toInt())
			setDimAmount(0.72f)
		}

		val reminders = fragment.get<ChannelFlowReminderScheduler>()
		val upcoming = program.startDate?.isAfter(LocalDateTime.now()) == true && program.channelId != null
		binding.remindButton.isVisible = upcoming
		if (upcoming) {
			fun refreshRemindLabel() {
				binding.remindButton.setText(
					if (reminders.isSet(program.id)) R.string.lbl_cancel_reminder else R.string.lbl_remind_me
				)
			}
			refreshRemindLabel()
			binding.remindButton.setOnClickListener {
				val nowSet = reminders.toggle(program, channelLabel(program))
				Toast.makeText(
					context,
					if (nowSet) R.string.msg_reminder_set else R.string.msg_reminder_cancelled,
					Toast.LENGTH_SHORT,
				).show()
				if (nowSet) dismiss() else refreshRemindLabel()
			}
		}

		binding.closeButton.setOnClickListener { dismiss() }

		dialog = window
		window.setOnShowListener {
			if (binding.remindButton.isVisible) binding.remindButton.requestFocus()
			else binding.closeButton.requestFocus()
		}
		window.show()
	}

	fun dismiss() {
		dialog?.dismiss()
		dialog = null
	}

	private fun bind(fragment: Fragment, binding: ProgramDetailDialogBinding, program: BaseItemDto) {
		val context = fragment.requireContext()
		val now = LocalDateTime.now()
		val upcoming = program.startDate?.isAfter(now) == true
		binding.status.setText(if (upcoming) R.string.lbl_upcoming else R.string.lbl_program_ended)
		binding.status.isVisible = !ChannelFlowGuideChrome.isAiringNow(program)

		binding.title.text = program.name.orEmpty()
		val episode = program.episodeTitle
		binding.episodeTitle.isVisible = !episode.isNullOrBlank()
		binding.episodeTitle.text = episode
		binding.channel.text = channelLabel(program)
		binding.infoRow.isVisible = false

		binding.schedule.text = listOfNotNull(
			scheduleLabel(fragment, program).takeIf { it.isNotBlank() },
			program.officialRating?.takeIf { it.isNotBlank() && it != "0" },
			program.productionYear?.toString(),
			program.genres.orEmpty().filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.joinToString(" · "),
		).joinToString("  ·  ")

		val overview = program.overview?.trim().orEmpty()
		binding.overview.text = overview.ifBlank { context.getString(R.string.no_program_data) }

		val catalog = fragment.get<ChannelFlowGuideRepository>()
		val imageHelper = fragment.get<ImageHelper>()
		val imageUrl = program.id.let(catalog::getLogoUrl)
			?: imageHelper.getPrimaryImageUrl(program, null, ImageHelper.MAX_PRIMARY_IMAGE_HEIGHT)
		binding.programImage.load(imageUrl, null, null, ImageHelper.ASPECT_RATIO_16_9, 32)
	}

	private fun channelLabel(program: BaseItemDto): String {
		val channel = program.channelId?.let { id ->
			TvManager.getAllChannels()?.firstOrNull { it.id == id }
		}
		return listOfNotNull(
			channel?.number,
			channel?.name ?: program.channelName,
		).joinToString("  ")
	}

	private fun scheduleLabel(fragment: Fragment, program: BaseItemDto): String {
		val context = fragment.requireContext()
		val start = program.startDate
		val end = program.endDate
		if (start == null || end == null) return ""
		val date = TimeUtils.getFriendlyDate(context, start)
		val range = context.getString(
			R.string.lbl_time_range,
			context.getTimeFormatter().format(start),
			context.getTimeFormatter().format(end),
		)
		val duration = TimeUtils.formatMillis(Duration.between(start, end).toMillis().coerceAtLeast(0))
		return listOf(date, range, duration).joinToString("  ·  ")
	}
}
