package com.futo.platformplayer.views.comments

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object YouTubeCommentsWebPolicy {
    const val YOUTUBE_SOURCE_ID = "35ae969a-a7db-11ed-afa1-0242ac120002"
    const val COMMENTS_ROOT_SELECTOR = "ytd-comments#comments"
    const val CHANNEL_NAVIGATION_SCHEME = "grayjay-comments"
    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

    private val videoIdRegex = Regex("^[A-Za-z0-9_-]{11}$")

    fun extractVideoId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = try {
            URI(url.trim())
        } catch (_: Throwable) {
            return null
        }

        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        val host = uri.host?.lowercase()?.trimEnd('.') ?: return null
        val pathSegments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }

        val candidate = when {
            host == "youtu.be" || host == "www.youtu.be" -> pathSegments.firstOrNull()
            isYouTubeHost(host) && pathSegments.firstOrNull() == "shorts" -> pathSegments.getOrNull(1)
            isYouTubeHost(host) && pathSegments.firstOrNull() == "embed" -> pathSegments.getOrNull(1)
            isYouTubeHost(host) && pathSegments.firstOrNull() == "live" -> pathSegments.getOrNull(1)
            isYouTubeHost(host) && (uri.path == "/watch" || uri.path == "watch") -> queryParameter(uri.rawQuery, "v")
            else -> null
        }

        return candidate?.takeIf { videoIdRegex.matches(it) }
    }

    fun canonicalDesktopWatchUrl(url: String?): String? {
        val videoId = extractVideoId(url) ?: return null
        return "https://www.youtube.com/watch?v=$videoId&app=desktop"
    }

    fun isSameVideoNavigation(candidateUrl: String?, expectedVideoId: String): Boolean {
        return extractVideoId(candidateUrl) == expectedVideoId
    }

    fun canonicalYouTubeChannelUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = try {
            URI(url.trim())
        } catch (_: Throwable) {
            return null
        }

        if (!uri.scheme.equals("https", ignoreCase = true) || !isYouTubeHost(uri.host))
            return null
        if (uri.userInfo != null || uri.port != -1) return null

        val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }
        val canonicalSegments = when {
            segments.firstOrNull()?.startsWith('@') == true &&
                isSafeChannelSegment(segments.first()) && segments.first().length > 1 ->
                listOf(segments.first())
            segments.firstOrNull() in setOf("channel", "c", "user") &&
                segments.getOrNull(1)?.let(::isSafeChannelSegment) == true ->
                listOf(segments.first(), segments[1])
            else -> return null
        }

        val canonicalPath = canonicalSegments.joinToString(separator = "/", prefix = "/")
        return try {
            URI("https", "www.youtube.com", canonicalPath, null).toASCIIString()
        } catch (_: Throwable) {
            null
        }
    }

    fun channelUrlFromSurfaceNavigation(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = try {
            URI(url.trim())
        } catch (_: Throwable) {
            return null
        }
        if (!uri.scheme.equals(CHANNEL_NAVIGATION_SCHEME, ignoreCase = true) ||
            !uri.host.equals("channel", ignoreCase = true))
            return null
        return canonicalYouTubeChannelUrl(queryParameter(uri.rawQuery, "url"))
    }

    fun isSurfaceNavigation(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            URI(url.trim()).scheme.equals(CHANNEL_NAVIGATION_SCHEME, ignoreCase = true)
        } catch (_: Throwable) {
            false
        }
    }

    fun isYouTubeHost(host: String?): Boolean {
        val normalized = host?.lowercase()?.trimEnd('.') ?: return false
        return normalized == "youtube.com" || normalized.endsWith(".youtube.com")
    }

    fun isGoogleSignInHost(host: String?): Boolean {
        val normalized = host?.lowercase()?.trimEnd('.') ?: return false
        return normalized == "accounts.google.com"
    }

    fun isBlockedMediaHost(host: String?): Boolean {
        val normalized = host?.lowercase()?.trimEnd('.') ?: return false
        return normalized == "googlevideo.com" || normalized.endsWith(".googlevideo.com")
    }

    fun isAllowedCookieDomain(domain: String?): Boolean {
        val normalized = domain?.lowercase()?.trim()?.trimStart('.')?.trimEnd('.') ?: return false
        return normalized == "youtube.com" || normalized.endsWith(".youtube.com") ||
            normalized == "google.com" || normalized.endsWith(".google.com")
    }

    private fun isSafeChannelSegment(segment: String): Boolean {
        return segment.isNotBlank() && segment.length <= 200 && segment != "." && segment != ".." &&
            segment.none { it.isISOControl() || it == '/' || it == '\\' || it == '?' || it == '#' }
    }

    private fun queryParameter(rawQuery: String?, name: String): String? {
        if (rawQuery.isNullOrEmpty()) return null
        return rawQuery.split('&').firstNotNullOfOrNull { entry ->
            val split = entry.split('=', limit = 2)
            if (split.isEmpty()) return@firstNotNullOfOrNull null
            val key = URLDecoder.decode(split[0], StandardCharsets.UTF_8.name())
            if (key != name) return@firstNotNullOfOrNull null
            URLDecoder.decode(split.getOrElse(1) { "" }, StandardCharsets.UTF_8.name())
        }
    }
}
