package com.futo.platformplayer.api.media.models.chapters

import kotlin.math.max
import kotlin.math.roundToLong

data class TimelineSegment(val startMs: Long, val endMs: Long, val color: Int) {
    val durationMs: Long get() = endMs - startMs
}

object TimelineSegments {
    fun fromChapters(chapters: List<IChapter>?): List<TimelineSegment> {
        val ordered = chapters.orEmpty().mapNotNull { chapter ->
            val color = chapter.timelineColor ?: return@mapNotNull null
            if(!chapter.timeStart.isFinite() || !chapter.timeEnd.isFinite() || chapter.timeEnd <= chapter.timeStart)
                return@mapNotNull null
            val start = (chapter.timeStart * 1000.0).roundToLong()
            val end = (chapter.timeEnd * 1000.0).roundToLong()
            if(end <= start) null else TimelineSegment(start, end, color)
        }.sortedWith(compareBy<TimelineSegment> { it.startMs }.thenBy { it.endMs })

        val merged = mutableListOf<TimelineSegment>()
        for(colorGroup in ordered.groupBy { it.color }.values) {
            val sameColor = mutableListOf<TimelineSegment>()
            for(segment in colorGroup) {
                val previous = sameColor.lastOrNull()
                val touchesPrevious = previous != null && (
                    segment.startMs <= previous.endMs ||
                        (previous.endMs != Long.MAX_VALUE && segment.startMs == previous.endMs + 1L)
                    )
                if(previous != null && touchesPrevious) {
                    sameColor[sameColor.lastIndex] = previous.copy(endMs = max(previous.endMs, segment.endMs))
                } else {
                    sameColor.add(segment)
                }
            }
            merged.addAll(sameColor)
        }
        // SponsorBlock draws longer overlaps first so shorter, more specific ranges remain visible.
        return merged.sortedWith(compareByDescending<TimelineSegment> { it.durationMs }.thenBy { it.startMs })
    }

    fun clipToDuration(segments: List<TimelineSegment>, durationMs: Long): List<TimelineSegment> {
        if(durationMs <= 0L) return emptyList()
        return segments.mapNotNull { segment ->
            val start = segment.startMs.coerceIn(0L, durationMs)
            val end = segment.endMs.coerceIn(0L, durationMs)
            if(end <= start) null else segment.copy(startMs = start, endMs = end)
        }
    }
}
