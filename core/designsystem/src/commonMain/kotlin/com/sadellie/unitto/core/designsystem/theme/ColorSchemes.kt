/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2025 Elshan Agaev
 * Copyright (c) 2026 Calculator-Pascaline (modifications)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sadellie.unitto.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.getValue

/** Pascaline colors: deep blue with a warm orange accent. */
val LightThemeColors: ColorScheme by lazy {
  ColorScheme(
    primary = Color(0xFF2F5DA8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF0F2F66),
    secondary = Color(0xFF565E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE3F4),
    onSecondaryContainer = Color(0xFF1A2333),
    tertiary = Color(0xFFE07A1F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF4A2300),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF1A1C20),
    surface = Color(0xFFFAFAFC),
    onSurface = Color(0xFF1A1C20),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFF1F0F7),
    inversePrimary = Color(0xFFADC6FF),
    surfaceDim = Color(0xFFD9DAE0),
    surfaceBright = Color(0xFFFAFAFC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F5FA),
    surfaceContainer = Color(0xFFEEF0F6),
    surfaceContainerHigh = Color(0xFFE8EAF1),
    surfaceContainerHighest = Color(0xFFE2E4EC),
    surfaceTint = Color(0xFF2F5DA8),
    primaryFixed = Color(0xFFD8E2FF),
    primaryFixedDim = Color(0xFFADC6FF),
    onPrimaryFixed = Color(0xFF001A41),
    onPrimaryFixedVariant = Color(0xFF1E4690),
    secondaryFixed = Color(0xFFDDE3F4),
    secondaryFixedDim = Color(0xFFC1C7D8),
    onSecondaryFixed = Color(0xFF161C2A),
    onSecondaryFixedVariant = Color(0xFF414858),
    tertiaryFixed = Color(0xFFFFDCC2),
    tertiaryFixedDim = Color(0xFFFFB77F),
    onTertiaryFixed = Color(0xFF2E1500),
    onTertiaryFixedVariant = Color(0xFF6E3900),
  )
}

/** Dark version of [LightThemeColors]. */
val DarkThemeColors: ColorScheme by lazy {
  ColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF0B2E66),
    primaryContainer = Color(0xFF26467F),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFC1C7D8),
    onSecondary = Color(0xFF2B3141),
    secondaryContainer = Color(0xFF2C3344),
    onSecondaryContainer = Color(0xFFDDE3F4),
    tertiary = Color(0xFFF59A45),
    onTertiary = Color(0xFF3A1A00),
    tertiaryContainer = Color(0xFF6E3900),
    onTertiaryContainer = Color(0xFFFFDCC2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E5EC),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE3E5EC),
    surfaceVariant = Color(0xFF2A2D35),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E5EC),
    inverseOnSurface = Color(0xFF2F3036),
    inversePrimary = Color(0xFF2F5DA8),
    surfaceDim = Color(0xFF111318),
    surfaceBright = Color(0xFF37393F),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191B21),
    surfaceContainer = Color(0xFF1D2027),
    surfaceContainerHigh = Color(0xFF272A31),
    surfaceContainerHighest = Color(0xFF32353C),
    surfaceTint = Color(0xFFADC6FF),
    primaryFixed = Color(0xFFD8E2FF),
    primaryFixedDim = Color(0xFFADC6FF),
    onPrimaryFixed = Color(0xFF001A41),
    onPrimaryFixedVariant = Color(0xFF1E4690),
    secondaryFixed = Color(0xFFDDE3F4),
    secondaryFixedDim = Color(0xFFC1C7D8),
    onSecondaryFixed = Color(0xFF161C2A),
    onSecondaryFixedVariant = Color(0xFF414858),
    tertiaryFixed = Color(0xFFFFDCC2),
    tertiaryFixedDim = Color(0xFFFFB77F),
    onTertiaryFixed = Color(0xFF2E1500),
    onTertiaryFixedVariant = Color(0xFF6E3900),
  )
}

fun Color.isDark(): Boolean = luminance() < LIGHT_OR_DARK_LUMINANCE_THRESHOLD

private const val LIGHT_OR_DARK_LUMINANCE_THRESHOLD = 0.5f
