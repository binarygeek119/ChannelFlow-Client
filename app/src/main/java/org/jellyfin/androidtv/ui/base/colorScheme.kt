package org.jellyfin.androidtv.ui.base

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

fun colorScheme(): ColorScheme = ColorScheme(
	background = Color(0xFF101010),
	onBackground = Color(0xFFEEEEEE),
	button = Color(0xFF161616),
	onButton = Color(0xFFEEEEEE),
	buttonFocused = Color(0xFFE11D48),
	onButtonFocused = Color(0xFFFFFFFF),
	buttonDisabled = Color(0x33161616),
	onButtonDisabled = Color(0xFF9B9B9B),
	buttonActive = Color(0xFFE11D48),
	onButtonActive = Color(0xFFFFFFFF),
	input = Color(0xFF111111),
	onInput = Color(0xFFEEEEEE),
	inputFocused = Color(0xFFE11D48),
	onInputFocused = Color(0xFFFFFFFF),
	rangeControlBackground = Color(0xFF2A2A2A),
	rangeControlFill = Color(0xFFE11D48),
	rangeControlKnob = Color(0xFFEEEEEE),
	seekbarBuffer = Color(0xFF9B9B9B),
	recording = Color(0xFFFB7185),
	onRecording = Color(0xFF101010),
	badge = Color(0xFFE11D48),
	onBadge = Color(0xFFFFFFFF),
	listHeader = Color(0xFFEEEEEE),
	listOverline = Color(0xFF9B9B9B),
	listHeadline = Color(0xFFEEEEEE),
	listCaption = Color(0xFF9B9B9B),
	listButton = Color.Transparent,
	listButtonFocused = Color(0xFFE11D48),
	surface = Color(0xFF181818),
	scrim = Color(0xAA000000),
)

@Immutable
data class ColorScheme(
	val background: Color,
	val onBackground: Color,

	val button: Color,
	val onButton: Color,
	val buttonFocused: Color,
	val onButtonFocused: Color,
	val buttonDisabled: Color,
	val onButtonDisabled: Color,
	val buttonActive: Color,
	val onButtonActive: Color,

	val input: Color,
	val onInput: Color,
	val inputFocused: Color,
	val onInputFocused: Color,

	val rangeControlBackground: Color,
	val rangeControlFill: Color,
	val rangeControlKnob: Color,
	val seekbarBuffer: Color,

	val recording: Color,
	val onRecording: Color,

	val badge: Color,
	val onBadge: Color,

	val listHeader: Color,
	val listOverline: Color,
	val listHeadline: Color,
	val listCaption: Color,
	val listButton: Color,
	val listButtonFocused: Color,

	val surface: Color,
	val scrim: Color,
)

val LocalColorScheme = staticCompositionLocalOf { colorScheme() }
