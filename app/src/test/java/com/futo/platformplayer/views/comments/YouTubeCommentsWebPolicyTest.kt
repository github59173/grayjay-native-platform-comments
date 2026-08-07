package com.futo.platformplayer.views.comments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeCommentsWebPolicyTest {
    @Test
    fun normalizesSupportedYouTubeVideoUrls() {
        val expected = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&app=desktop"
        assertEquals(expected, YouTubeCommentsWebPolicy.canonicalDesktopWatchUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(expected, YouTubeCommentsWebPolicy.canonicalDesktopWatchUrl("https://youtu.be/dQw4w9WgXcQ?t=42"))
        assertEquals(expected, YouTubeCommentsWebPolicy.canonicalDesktopWatchUrl("https://youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals(expected, YouTubeCommentsWebPolicy.canonicalDesktopWatchUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun rejectsInvalidOrUntrustedUrls() {
        assertNull(YouTubeCommentsWebPolicy.canonicalDesktopWatchUrl("http://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertNull(YouTubeCommentsWebPolicy.canonicalDesktopWatchUrl("https://youtube.example/watch?v=dQw4w9WgXcQ"))
        assertNull(YouTubeCommentsWebPolicy.canonicalDesktopWatchUrl("https://www.youtube.com/watch?v=too-short"))
        assertNull(YouTubeCommentsWebPolicy.canonicalDesktopWatchUrl("not a url"))
    }

    @Test
    fun limitsMediaAndCookieHosts() {
        assertTrue(YouTubeCommentsWebPolicy.isBlockedMediaHost("rr1---sn.googlevideo.com"))
        assertFalse(YouTubeCommentsWebPolicy.isBlockedMediaHost("youtubei.googleapis.com"))
        assertTrue(YouTubeCommentsWebPolicy.isAllowedCookieDomain(".youtube.com"))
        assertTrue(YouTubeCommentsWebPolicy.isAllowedCookieDomain("accounts.google.com"))
        assertFalse(YouTubeCommentsWebPolicy.isAllowedCookieDomain("example.com"))
    }

    @Test
    fun normalizesSupportedYouTubeChannelUrls() {
        assertEquals(
            "https://www.youtube.com/@YouTube",
            YouTubeCommentsWebPolicy.canonicalYouTubeChannelUrl(
                "https://m.youtube.com/@YouTube/videos?view=0"
            )
        )
        assertEquals(
            "https://www.youtube.com/channel/UC_x5XG1OV2P6uZZ5FSM9Ttw",
            YouTubeCommentsWebPolicy.canonicalYouTubeChannelUrl(
                "https://youtube.com/channel/UC_x5XG1OV2P6uZZ5FSM9Ttw/featured"
            )
        )
        assertEquals(
            "https://www.youtube.com/user/Google",
            YouTubeCommentsWebPolicy.canonicalYouTubeChannelUrl("https://www.youtube.com/user/Google")
        )
    }

    @Test
    fun validatesCommentsSurfaceChannelNavigation() {
        val routed = "grayjay-comments://channel?url=" +
            "https%3A%2F%2Fwww.youtube.com%2F%40YouTube%2Fvideos%3Fview%3D0"
        assertEquals(
            "https://www.youtube.com/@YouTube",
            YouTubeCommentsWebPolicy.channelUrlFromSurfaceNavigation(routed)
        )
        assertTrue(YouTubeCommentsWebPolicy.isSurfaceNavigation(routed))

        assertNull(
            YouTubeCommentsWebPolicy.channelUrlFromSurfaceNavigation(
                "grayjay-comments://channel?url=https%3A%2F%2Fevil.example%2F%40YouTube"
            )
        )
        assertNull(
            YouTubeCommentsWebPolicy.channelUrlFromSurfaceNavigation(
                "grayjay-comments://other?url=https%3A%2F%2Fwww.youtube.com%2F%40YouTube"
            )
        )
        assertNull(YouTubeCommentsWebPolicy.canonicalYouTubeChannelUrl("http://youtube.com/@YouTube"))
        assertNull(YouTubeCommentsWebPolicy.canonicalYouTubeChannelUrl("https://youtube.example/@YouTube"))
        assertNull(YouTubeCommentsWebPolicy.canonicalYouTubeChannelUrl("https://youtube.com/watch?v=dQw4w9WgXcQ"))
    }
}
