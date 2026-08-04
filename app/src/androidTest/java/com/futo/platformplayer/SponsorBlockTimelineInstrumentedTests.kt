package com.futo.platformplayer

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futo.platformplayer.views.fields.ColorSwatchView
import com.futo.platformplayer.views.fields.DropdownField
import com.futo.platformplayer.views.video.SegmentedTimeBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class SponsorBlockTimelineInstrumentedTests {
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.Theme_FutoVideo
    )

    @Test
    fun dropdownDisplaysConfiguredInlineColor() {
        val field = DropdownField(context)
            .withValue("Skip Sponsors", "fixture", listOf("No skip", "Manual", "Automatic"), 1)
            .withInlineColor("#40010203", "#B300D400", true)
        val swatch = field.findViewById<ColorSwatchView>(R.id.field_inline_color)
        assertEquals(View.VISIBLE, swatch.visibility)
        assertEquals(0x40010203, swatch.getColor())
    }

    @Test
    fun allRegularPlaybackLayoutsUseSegmentedTimeBars() {
        val inflater = LayoutInflater.from(context)
        val layoutsAndIds = listOf(
            R.layout.video_player_ui to R.id.time_progress,
            R.layout.video_player_ui_fullscreen to R.id.time_progress,
            R.layout.video_player_ui_bar to androidx.media3.ui.R.id.exo_progress
        )
        layoutsAndIds.forEach { (layout, id) ->
            val root = inflater.inflate(layout, null, false)
            assertTrue("Layout $layout should use SegmentedTimeBar", root.findViewById<View>(id) is SegmentedTimeBar)
        }

        val portrait = inflater.inflate(R.layout.video_player_ui, null, false)
        val portraitTimeBar = portrait.findViewById<SegmentedTimeBar>(R.id.time_progress)
        assertTrue(
            "The transparent portrait scrubber must not duplicate the persistent timeline segments",
            !portraitTimeBar.isSegmentOverlayEnabled()
        )

        // The cast layout's existing GestureControlView requires a live activity/window and cannot be
        // inflated in isolation. Inspect its compiled XML declaration instead of bypassing that contract.
        val parser = context.resources.getLayout(R.layout.view_cast)
        var foundCastTimeBar = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG &&
                parser.name == SegmentedTimeBar::class.java.name &&
                parser.getAttributeResourceValue(ANDROID_NAMESPACE, "id", 0) == R.id.time_progress
            ) {
                foundCastTimeBar = true
                break
            }
            parser.next()
        }
        parser.close()
        assertTrue("Cast controls should declare a SegmentedTimeBar", foundCastTimeBar)
    }

    companion object {
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
