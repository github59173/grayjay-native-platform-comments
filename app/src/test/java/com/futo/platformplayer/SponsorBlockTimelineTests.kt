package com.futo.platformplayer

import com.futo.platformplayer.api.media.models.chapters.ChapterType
import com.futo.platformplayer.api.media.models.chapters.IChapter
import com.futo.platformplayer.api.media.models.chapters.TimelineColor
import com.futo.platformplayer.api.media.models.chapters.TimelineSegment
import com.futo.platformplayer.api.media.models.chapters.TimelineSegments
import com.futo.platformplayer.api.media.platforms.js.SourcePluginConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SponsorBlockTimelineTests {
    private fun chapter(start: Double, end: Double, color: Int?) = object : IChapter {
        override val name = "fixture"
        override val type = ChapterType.SKIPPABLE
        override val timeStart = start
        override val timeEnd = end
        override val timelineColor = color
    }

    @Test
    fun timelineColorParsesAndNormalizesArgb() {
        assertEquals(0xB300D400.toInt(), TimelineColor.parse("#B300D400"))
        assertEquals(0xFF00D400.toInt(), TimelineColor.parse("#00D400"))
        assertEquals("#B300D400", TimelineColor.normalize("#b300d400"))
        assertNull(TimelineColor.parse("green"))
    }

    @Test
    fun segmentBuilderRejectsInvalidAndUncoloredChapters() {
        val segments = TimelineSegments.fromChapters(listOf(
            chapter(0.0, 1.0, null),
            chapter(2.0, 1.0, 0xFF00FF00.toInt()),
            chapter(Double.NaN, 3.0, 0xFF00FF00.toInt()),
            chapter(4.0, 5.0, 0xFF00FF00.toInt())
        ))
        assertEquals(1, segments.size)
        assertEquals(4000L, segments.single().startMs)
    }

    @Test
    fun segmentBuilderMergesAdjacentColorsAndDrawsShorterOverlapsLast() {
        val green = 0xB300D400.toInt()
        val blue = 0xB3008FD6.toInt()
        val segments = TimelineSegments.fromChapters(listOf(
            chapter(0.0, 5.0, green),
            chapter(5.0, 10.0, green),
            chapter(2.0, 4.0, blue)
        ))
        assertEquals(2, segments.size)
        assertEquals(green, segments[0].color)
        assertEquals(10_000L, segments[0].endMs)
        assertEquals(blue, segments[1].color)
    }

    @Test
    fun segmentBuilderClipsRangesToKnownDuration() {
        val color = 0xB300D400.toInt()
        val clipped = TimelineSegments.clipToDuration(listOf(
            TimelineSegment(-1_000L, 2_000L, color),
            TimelineSegment(9_000L, 12_000L, color),
            TimelineSegment(11_000L, 12_000L, color)
        ), 10_000L)
        assertEquals(listOf(0L, 9_000L), clipped.map { it.startMs })
        assertEquals(listOf(2_000L, 10_000L), clipped.map { it.endMs })
    }

    @Test
    fun legacyChapterImplementationsRemainUncolored() {
        val legacyChapter = object : IChapter {
            override val name = "legacy"
            override val type = ChapterType.NORMAL
            override val timeStart = 0.0
            override val timeEnd = 1.0
        }
        assertNull(legacyChapter.timelineColor)
    }

    @Test
    fun inlineColorDefaultsAreHydratedAlongsideTheDropdown() {
        val setting = SourcePluginConfig.Setting(
            name = "Skip Sponsors",
            description = "fixture",
            type = "Dropdown",
            default = "1",
            variable = "sponsorMode",
            inlineColor = SourcePluginConfig.InlineColorSetting(
                variable = "sponsorColor",
                default = "#B300D400",
                allowAlpha = true
            )
        )
        val values = mutableMapOf<String, String?>()
        setting.applyDefaults(values)
        assertEquals("1", values["sponsorMode"])
        assertEquals("\"#B300D400\"", values["sponsorColor"])
        assertEquals("#B300D400", setting.inlineColor!!.fromStoredValue(values["sponsorColor"]))
    }

    @Test
    fun rawInlineColorsFromDevelopmentBuildsAreMigratedToJsonLiterals() {
        val color = SourcePluginConfig.InlineColorSetting(
            variable = "sponsorColor",
            default = "#B300D400",
            allowAlpha = true
        )
        assertEquals("\"#40010203\"", color.toStoredValue("#40010203"))
        assertEquals("#40010203", color.fromStoredValue("\"#40010203\""))
    }
}
