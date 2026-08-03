package com.futo.platformplayer

import android.content.Context
import androidx.core.content.ContextCompat
import com.futo.platformplayer.api.media.IPlatformClient

/**
 * Resolves the source-owned UI accent without embedding platform checks in each view.
 * Sources without an accent retain Grayjay's neutral primary-color treatment.
 */
fun IPlatformClient.resolvePlatformAccentColor(context: Context): Int =
    accentColor ?: ContextCompat.getColor(context, R.color.colorPrimary)
