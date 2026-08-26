package org.jellyfin.androidtv.ui.settings

import org.jellyfin.androidtv.ui.navigation.RouteComposable
import org.jellyfin.androidtv.ui.settings.screen.SettingsDeveloperScreen
import org.jellyfin.androidtv.ui.settings.screen.SettingsMainScreen
import org.jellyfin.androidtv.ui.settings.screen.SettingsTelemetryScreen
import org.jellyfin.androidtv.ui.settings.screen.about.SettingsAboutScreen
import org.jellyfin.androidtv.ui.settings.screen.connection.SettingsConnectionScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.SettingsCustomizationBackdropScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.SettingsCustomizationClockScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.SettingsCustomizationScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.SettingsCustomizationThemeScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.SettingsSubtitleTextStrokeColorScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.SettingsSubtitlesBackgroundColorScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.SettingsSubtitlesScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.SettingsSubtitlesTextColorScreen
import org.jellyfin.androidtv.ui.settings.screen.license.SettingsLicenseScreen
import org.jellyfin.androidtv.ui.settings.screen.license.SettingsLicensesScreen
import org.jellyfin.androidtv.ui.settings.screen.livetv.SettingsLiveTvGuideChannelOrderScreen
import org.jellyfin.androidtv.ui.settings.screen.livetv.SettingsLiveTvGuideFiltersScreen
import org.jellyfin.androidtv.ui.settings.screen.livetv.SettingsLiveTvGuideOptionsScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackAVCLevelScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackAdvancedScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackAudioBehaviorScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackBufferLengthScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackCodecScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackHEVCLevelScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackMaxBitrateScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackPlayerScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackRefreshRateSwitchingBehaviorScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackResumeSubtractDurationScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackZoomModeScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment.SettingsPlaybackMediaSegmentScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment.SettingsPlaybackMediaSegmentsScreen
import org.jellyfin.sdk.model.api.MediaSegmentType

object Routes {
	const val MAIN = "/"
	const val CONNECTION = "/connection"
	const val CUSTOMIZATION = "/customization"
	const val CUSTOMIZATION_THEME = "/customization/theme"
	const val CUSTOMIZATION_CLOCK = "/customization/clock"
	const val CUSTOMIZATION_BACKDROP = "/customization/backdrop"
	const val CUSTOMIZATION_SUBTITLES = "/customization/subtitles"
	const val CUSTOMIZATION_SUBTITLES_TEXT_COLOR = "/customization/subtitles/text-color"
	const val CUSTOMIZATION_SUBTITLES_BACKGROUND_COLOR = "/customization/subtitles/background-color"
	const val CUSTOMIZATION_SUBTITLES_EDGE_COLOR = "/customization/subtitles/edge-color"
	const val LIVETV_GUIDE_FILTERS = "/livetv/guide/filters"
	const val LIVETV_GUIDE_OPTIONS = "/livetv/guide/options"
	const val LIVETV_GUIDE_CHANNEL_ORDER = "/livetv/guide/channel-order"
	const val PLAYBACK = "/playback"
	const val PLAYBACK_PLAYER = "/playback/player"
	const val PLAYBACK_MEDIA_SEGMENTS = "/playback/media-segments"
	const val PLAYBACK_MEDIA_SEGMENT = "/playback/media-segments/{segmentType}"
	const val PLAYBACK_ADVANCED = "/playback/advanced"
	const val PLAYBACK_RESUME_SUBTRACT_DURATION = "/playback/resume-subtract-duration"
	const val PLAYBACK_MAX_BITRATE = "/playback/max-bitrate"
	const val PLAYBACK_REFRESH_RATE_SWITCHING_BEHAVIOR = "/playback/refresh-rate-switching-behavior"
	const val PLAYBACK_ZOOM_MODE = "/playback/zoom-mode"
	const val PLAYBACK_BUFFER_LENGTH = "/playback/buffer-length"
	const val PLAYBACK_AUDIO_BEHAVIOR = "/playback/audio-behavior"
	const val PLAYBACK_CODEC = "/playback/codec"
	const val PLAYBACK_AVC_LEVEL = "/playback/codec/avc-level"
	const val PLAYBACK_HEVC_LEVEL = "/playback/codec/hevc-level"
	const val TELEMETRY = "/telemetry"
	const val DEVELOPER = "/developer"
	const val ABOUT = "/about"
	const val LICENSES = "/licenses"
	const val LICENSE = "/license/{artifactId}"
}

val routes = mapOf<String, RouteComposable>(
	Routes.MAIN to {
		SettingsMainScreen()
	},
	Routes.CONNECTION to {
		SettingsConnectionScreen()
	},
	Routes.CUSTOMIZATION to {
		SettingsCustomizationScreen()
	},
	Routes.CUSTOMIZATION_THEME to {
		SettingsCustomizationThemeScreen()
	},
	Routes.CUSTOMIZATION_CLOCK to {
		SettingsCustomizationClockScreen()
	},
	Routes.CUSTOMIZATION_BACKDROP to {
		SettingsCustomizationBackdropScreen()
	},
	Routes.CUSTOMIZATION_SUBTITLES to {
		SettingsSubtitlesScreen()
	},
	Routes.CUSTOMIZATION_SUBTITLES_TEXT_COLOR to {
		SettingsSubtitlesTextColorScreen()
	},
	Routes.CUSTOMIZATION_SUBTITLES_BACKGROUND_COLOR to {
		SettingsSubtitlesBackgroundColorScreen()
	},
	Routes.CUSTOMIZATION_SUBTITLES_EDGE_COLOR to {
		SettingsSubtitleTextStrokeColorScreen()
	},
	Routes.LIVETV_GUIDE_FILTERS to {
		SettingsLiveTvGuideFiltersScreen()
	},
	Routes.LIVETV_GUIDE_OPTIONS to {
		SettingsLiveTvGuideOptionsScreen()
	},
	Routes.LIVETV_GUIDE_CHANNEL_ORDER to {
		SettingsLiveTvGuideChannelOrderScreen()
	},
	Routes.PLAYBACK to {
		SettingsPlaybackScreen()
	},
	Routes.PLAYBACK_PLAYER to {
		SettingsPlaybackPlayerScreen()
	},
	Routes.PLAYBACK_MEDIA_SEGMENTS to {
		SettingsPlaybackMediaSegmentsScreen()
	},
	Routes.PLAYBACK_MEDIA_SEGMENT to { context ->
		SettingsPlaybackMediaSegmentScreen(
			segmentType = context.parameters["segmentType"]?.let(MediaSegmentType::fromNameOrNull)!!,
		)
	},
	Routes.PLAYBACK_ADVANCED to {
		SettingsPlaybackAdvancedScreen()
	},
	Routes.PLAYBACK_RESUME_SUBTRACT_DURATION to {
		SettingsPlaybackResumeSubtractDurationScreen()
	},
	Routes.PLAYBACK_MAX_BITRATE to {
		SettingsPlaybackMaxBitrateScreen()
	},
	Routes.PLAYBACK_REFRESH_RATE_SWITCHING_BEHAVIOR to {
		SettingsPlaybackRefreshRateSwitchingBehaviorScreen()
	},
	Routes.PLAYBACK_ZOOM_MODE to {
		SettingsPlaybackZoomModeScreen()
	},
	Routes.PLAYBACK_BUFFER_LENGTH to {
		SettingsPlaybackBufferLengthScreen()
	},
	Routes.PLAYBACK_AUDIO_BEHAVIOR to {
		SettingsPlaybackAudioBehaviorScreen()
	},
	Routes.PLAYBACK_CODEC to {
		SettingsPlaybackCodecScreen()
	},
	Routes.PLAYBACK_AVC_LEVEL to {
		SettingsPlaybackAVCLevelScreen()
	},
	Routes.PLAYBACK_HEVC_LEVEL to {
		SettingsPlaybackHEVCLevelScreen()
	},
	Routes.TELEMETRY to {
		SettingsTelemetryScreen()
	},
	Routes.DEVELOPER to {
		SettingsDeveloperScreen()
	},
	Routes.ABOUT to {
		SettingsAboutScreen()
	},
	Routes.LICENSES to {
		SettingsLicensesScreen()
	},
	Routes.LICENSE to { context ->
		SettingsLicenseScreen(
			artifactId = context.parameters["artifactId"]!!
		)
	},
)
