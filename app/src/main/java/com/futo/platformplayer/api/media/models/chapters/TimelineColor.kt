package com.futo.platformplayer.api.media.models.chapters

object TimelineColor {
    private val argbPattern = Regex("^#[0-9a-fA-F]{8}$")
    private val rgbPattern = Regex("^#[0-9a-fA-F]{6}$")

    fun parse(value: String?): Int? {
        val normalized = value?.trim() ?: return null
        val digits = when {
            argbPattern.matches(normalized) -> normalized.substring(1)
            rgbPattern.matches(normalized) -> "FF${normalized.substring(1)}"
            else -> return null
        }
        return digits.toLongOrNull(16)?.toInt()
    }

    fun formatArgb(color: Int): String = "#%08X".format(color)

    fun normalize(value: String?): String? = parse(value)?.let(::formatArgb)
}
