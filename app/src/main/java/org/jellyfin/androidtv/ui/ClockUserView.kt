package org.jellyfin.androidtv.ui

import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.channelflow.ChannelFlowGuideRepository
import org.jellyfin.androidtv.databinding.ClockUserBugBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideFragment
import org.jellyfin.androidtv.ui.livetv.TvManager
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ClockUserView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), KoinComponent {
	private val binding: ClockUserBugBinding = ClockUserBugBinding.inflate(LayoutInflater.from(context), this, true)
	private val userPreferences by inject<UserPreferences>()
	private val navigationRepository by inject<NavigationRepository>()
	private val catalog by inject<ChannelFlowGuideRepository>()

	var isVideoPlayer = false
		set(value) {
			field = value
			updateClockVisibility()
		}

	val homeButton get() = binding.home

	init {
		updateClockVisibility()

		binding.home.setOnClickListener {
			navigationRepository.reset(Destinations.liveTvGuide, clearHistory = true)
		}

		binding.refreshGuide.setOnClickListener {
			refreshGuide()
		}

		binding.settings.setOnClickListener {
			context.findComponentActivity()?.getViewModel<SettingsViewModel>()?.show()
		}
	}

	private fun refreshGuide() {
		val activity = context.findComponentActivity() as? FragmentActivity ?: return
		if (!binding.refreshGuide.isEnabled) return
		binding.refreshGuide.isEnabled = false
		activity.lifecycleScope.launch {
			val result = runCatching { catalog.refresh(force = true) }
			result.onFailure { Timber.w(it, "Unable to refresh ChannelFlow guide") }
			if (result.isSuccess) {
				TvManager.forceReload()
				val guide = activity.findFragmentOfType(LiveTvGuideFragment::class.java)
				if (guide != null) {
					guide.reloadGuide()
				} else {
					navigationRepository.reset(Destinations.liveTvGuide, clearHistory = true)
				}
				Toast.makeText(context, R.string.msg_guide_updated, Toast.LENGTH_SHORT).show()
			}
			binding.refreshGuide.isEnabled = true
		}
	}

	private fun updateClockVisibility() {
		val showClock = userPreferences[UserPreferences.clockBehavior]

		binding.clock.isVisible = when (showClock) {
			ClockBehavior.ALWAYS -> true
			ClockBehavior.NEVER -> false
			ClockBehavior.IN_VIDEO -> isVideoPlayer
			ClockBehavior.IN_MENUS -> !isVideoPlayer
		}

		binding.home.isVisible = !isVideoPlayer
		binding.refreshGuide.isVisible = !isVideoPlayer
		binding.settings.isVisible = !isVideoPlayer
	}
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
	is ComponentActivity -> this
	is ContextWrapper -> baseContext.findComponentActivity()
	else -> null
}

private fun <T : Fragment> FragmentActivity.findFragmentOfType(type: Class<T>): T? {
	fun search(fragments: List<Fragment>): T? {
		fragments.forEach { fragment ->
			if (type.isInstance(fragment)) return type.cast(fragment)
			search(fragment.childFragmentManager.fragments)?.let { return it }
		}
		return null
	}
	return search(supportFragmentManager.fragments)
}
