package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.getQualityProfiles
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.form.RangeControl
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListControl
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.design.Tokens
import org.koin.compose.koinInject
import java.text.DecimalFormat
import kotlin.math.roundToLong

@Composable
fun SettingsPlaybackAdvancedScreen() {
	val context = LocalContext.current
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback_advanced)) },
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.pref_customization)) }) }

		item {
			var bufferLength by rememberPreference(userPreferences, UserPreferences.bufferLength)

			ListButton(
				headingContent = { Text(stringResource(R.string.playback_buffer_length)) },
				captionContent = { Text(stringResource(bufferLength.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_BUFFER_LENGTH) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_BUFFER_LENGTH)
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.pref_video)) }) }

		item {
			var maxBitrate by rememberPreference(userPreferences, UserPreferences.maxBitrate)
			val options = getQualityProfiles(context)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_max_bitrate_title)) },
				captionContent = { Text(options[maxBitrate].orEmpty()) },
				onClick = { router.push(Routes.PLAYBACK_MAX_BITRATE) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_MAX_BITRATE)
			)
		}

		item {
			var refreshRateSwitchingBehavior by rememberPreference(userPreferences, UserPreferences.refreshRateSwitchingBehavior)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_refresh_switching)) },
				captionContent = { Text(stringResource(refreshRateSwitchingBehavior.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_REFRESH_RATE_SWITCHING_BEHAVIOR) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_REFRESH_RATE_SWITCHING_BEHAVIOR)
			)
		}

		item {
			var videoStartDelay by rememberPreference(userPreferences, UserPreferences.videoStartDelay)
			val interactionSource = remember { MutableInteractionSource() }

			ListControl(
				headingContent = { Text(stringResource(R.string.video_start_delay)) },
				interactionSource = interactionSource,
				modifier = Modifier.focusKey("video_start_delay")
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
				) {
					RangeControl(
						modifier = Modifier
							.height(4.dp)
							.weight(1f),
						interactionSource = interactionSource,
						// 0 - 5 seconds with 0.25 second increment
						min = 0_000f,
						max = 5_000f,
						stepForward = 250f,
						value = videoStartDelay.toFloat(),
						onValueChange = { videoStartDelay = it.roundToLong() }
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 48.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						val formatter = remember { DecimalFormat("0.##") }
						Text("${formatter.format(videoStartDelay / 1000f)}s")
					}
				}
			}
		}

		item {
			var playerZoomMode by rememberPreference(userPreferences, UserPreferences.playerZoomMode)

			ListButton(
				headingContent = { Text(stringResource(R.string.default_video_zoom)) },
				captionContent = { Text(stringResource(playerZoomMode.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_ZOOM_MODE) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_ZOOM_MODE)
			)
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.preference_codecs)) },
				captionContent = { Text(stringResource(R.string.preference_codecs_summary)) },
				onClick = { router.push(Routes.PLAYBACK_CODEC) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_CODEC)
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.pref_subtitles)) }) }

		item {
			var pgsDirectPlay by rememberPreference(userPreferences, UserPreferences.pgsDirectPlay)

			ListButton(
				headingContent = { Text(stringResource(R.string.preference_enable_pgs)) },
				captionContent = { Text(stringResource(R.string.preference_enable_pgs_description)) },
				trailingContent = { Checkbox(checked = pgsDirectPlay) },
				onClick = { pgsDirectPlay = !pgsDirectPlay },
				modifier = Modifier.focusKey("pgs_direct_play")
			)
		}

		item {
			var assDirectPlay by rememberPreference(userPreferences, UserPreferences.assDirectPlay)

			ListButton(
				headingContent = { Text(stringResource(R.string.preference_enable_ass)) },
				captionContent = { Text(stringResource(R.string.preference_enable_ass_description)) },
				trailingContent = { Checkbox(checked = assDirectPlay) },
				onClick = { assDirectPlay = !assDirectPlay },
				modifier = Modifier.focusKey("ass_direct_play")
			)
		}

		item {
			var subtitlesBurnDuringTranscode by rememberPreference(userPreferences, UserPreferences.subtitlesBurnDuringTranscode)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_burn_subtitles_when_transcoding)) },
				captionContent = { Text(stringResource(R.string.pref_burn_subtitles_when_transcoding_description)) },
				trailingContent = { Checkbox(checked = subtitlesBurnDuringTranscode) },
				onClick = { subtitlesBurnDuringTranscode = !subtitlesBurnDuringTranscode },
				modifier = Modifier.focusKey("subtitles_burn_during_transcode")
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.pref_audio)) }) }

		item {
			var audioBehaviour by rememberPreference(userPreferences, UserPreferences.audioBehaviour)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_audio_output)) },
				captionContent = { Text(stringResource(audioBehaviour.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_AUDIO_BEHAVIOR) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_AUDIO_BEHAVIOR)
			)
		}

		item {
			var audioNightMode by rememberPreference(userPreferences, UserPreferences.audioNightMode)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_audio_night_mode)) },
				trailingContent = { Checkbox(checked = audioNightMode) },
				onClick = { audioNightMode = !audioNightMode },
				modifier = Modifier.focusKey("audio_night_mode")
			)
		}

		item {
			var ac3Enabled by rememberPreference(userPreferences, UserPreferences.ac3Enabled)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_bitstream_ac3)) },
				trailingContent = { Checkbox(checked = ac3Enabled) },
				onClick = { ac3Enabled = !ac3Enabled },
				modifier = Modifier.focusKey("ac3_enabled")
			)
		}
	}
}
