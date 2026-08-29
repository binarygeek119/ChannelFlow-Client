package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class PlaybackEngine(
	override val nameRes: Int,
) : PreferenceEnum {
	EXOPLAYER(R.string.video_player_exoplayer),
	VLC(R.string.video_player_vlc),
}
