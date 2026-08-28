package org.jellyfin.androidtv.channelflow

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.sdk.model.api.BaseItemDto

object ChannelFlowGuideChrome {
	fun programBackground(context: Context, program: BaseItemDto, focused: Boolean): Drawable {
		val now = isAiringNow(program)
		val fill = when {
			focused -> color(context, R.color.channelflow_accent)
			now -> color(context, R.color.channelflow_guide_now)
			else -> color(context, R.color.channelflow_guide_block)
		}
		val stripe = when {
			focused -> Color.WHITE
			now -> color(context, R.color.channelflow_accent)
			else -> stripeColor(context, program)
		}
		return block(context, fill, stripe)
	}

	fun channelBackground(context: Context, focused: Boolean): Drawable {
		val fill = if (focused) color(context, R.color.channelflow_accent) else color(context, R.color.channelflow_drawer)
		val stripe = if (focused) Color.WHITE else color(context, R.color.channelflow_border)
		return block(context, fill, stripe)
	}

	fun isAiringNow(program: BaseItemDto): Boolean {
		val start = program.startDate ?: return false
		val end = program.endDate ?: return false
		val now = ChannelFlowGuideClock.now()
		return !now.isBefore(start) && now.isBefore(end)
	}

	private fun stripeColor(context: Context, program: BaseItemDto): Int {
		val haystack = buildString {
			append(program.name.orEmpty())
			append(' ')
			append(program.genres.orEmpty().joinToString(" "))
			append(' ')
			append(program.episodeTitle.orEmpty())
		}.lowercase()
		return when {
			Utils.isTrue(program.isMovie) || haystack.contains("movie") -> color(context, R.color.channelflow_guide_movie)
			Utils.isTrue(program.isNews) || haystack.contains("news") -> color(context, R.color.channelflow_guide_news)
			haystack.contains("weather") -> color(context, R.color.channelflow_guide_weather)
			haystack.contains("music") -> color(context, R.color.channelflow_guide_music)
			Utils.isTrue(program.isSports) -> color(context, R.color.channelflow_guide_music)
			Utils.isTrue(program.isKids) || haystack.contains("series") || haystack.contains("show") ->
				color(context, R.color.channelflow_guide_tvshow)
			else -> color(context, R.color.channelflow_guide_stripe)
		}
	}

	private fun block(context: Context, fill: Int, stripe: Int): Drawable {
		val radius = Utils.convertDpToPixel(context, 4).toFloat()
		val background = GradientDrawable().apply {
			shape = GradientDrawable.RECTANGLE
			cornerRadius = radius
			setColor(fill)
		}
		val edge = GradientDrawable().apply {
			shape = GradientDrawable.RECTANGLE
			setColor(stripe)
		}
		return LayerDrawable(arrayOf(background, edge)).apply {
			setLayerGravity(1, Gravity.START)
			setLayerWidth(1, Utils.convertDpToPixel(context, 3))
			setLayerInset(0, 0, 0, 0, 0)
		}
	}

	private fun color(context: Context, id: Int) = ContextCompat.getColor(context, id)
}
