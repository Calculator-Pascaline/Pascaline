/*
 * Unitto is a calculator for Android
 * Copyright (c) 2024-2025 Elshan Agaev
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

package io.github.calculatorpascaline.feature.glance.common

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.glance.GlanceComposable
import androidx.glance.LocalContext
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import io.github.calculatorpascaline.core.designsystem.theme.DarkThemeColors
import io.github.calculatorpascaline.core.designsystem.theme.LightThemeColors
import io.github.sadellie.themmo.toAmoled

@Composable
internal fun WidgetTheme(isAmoledEnabled: Boolean, content: @Composable () -> Unit) {
  val colors = customColorProviders(LocalContext.current, isAmoledEnabled)
  CompositionLocalProvider(LocalPascalineGlanceColors provides colors, content = content)
}

object PascalineGlanceTheme {
  val colors: PascalineColorProviders
    @GlanceComposable @Composable @ReadOnlyComposable get() = LocalPascalineGlanceColors.current
}

internal val LocalPascalineGlanceColors: ProvidableCompositionLocal<PascalineColorProviders> =
  staticCompositionLocalOf {
    pascalineColorProviders(LightThemeColors, DarkThemeColors)
  }

data class PascalineColorProviders(
  val primary: ColorProvider,
  val onPrimary: ColorProvider,
  val primaryContainer: ColorProvider,
  val onPrimaryContainer: ColorProvider,
  val secondaryContainer: ColorProvider,
  val onSecondaryContainer: ColorProvider,
  val inverseOnSurface: ColorProvider,
  val tertiaryContainer: ColorProvider,
  val onTertiaryContainer: ColorProvider,
  val onBackground: ColorProvider,
  val surfaceContainer: ColorProvider,
  val surfaceVariant: ColorProvider,
  val onSurfaceVariant: ColorProvider,
  val outline: ColorProvider,
)

private fun customColorProviders(context: Context, isAmoledEnabled: Boolean): PascalineColorProviders {
  val isApi34 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
  var dark = if (isApi34) dynamicDarkColorScheme(context) else DarkThemeColors
  if (isAmoledEnabled) dark = dark.toAmoled()
  val light = if (isApi34) dynamicLightColorScheme(context) else LightThemeColors
  return pascalineColorProviders(light, dark)
}

private fun pascalineColorProviders(light: ColorScheme, dark: ColorScheme) =
  PascalineColorProviders(
    primary = ColorProvider(light.primary, dark.primary),
    onPrimary = ColorProvider(light.onPrimary, dark.onPrimary),
    primaryContainer = ColorProvider(light.primaryContainer, dark.primaryContainer),
    onPrimaryContainer = ColorProvider(light.onPrimaryContainer, dark.onPrimaryContainer),
    surfaceContainer = ColorProvider(light.surfaceContainer, dark.surfaceContainer),
    surfaceVariant = ColorProvider(light.surfaceVariant, dark.surfaceVariant),
    onSurfaceVariant = ColorProvider(light.onSurfaceVariant, dark.onSurfaceVariant),
    secondaryContainer = ColorProvider(light.secondaryContainer, dark.secondaryContainer),
    onSecondaryContainer = ColorProvider(light.onSecondaryContainer, dark.onSecondaryContainer),
    inverseOnSurface = ColorProvider(light.inverseOnSurface, dark.inverseOnSurface),
    tertiaryContainer = ColorProvider(light.tertiaryContainer, dark.tertiaryContainer),
    onTertiaryContainer = ColorProvider(light.onTertiaryContainer, dark.onTertiaryContainer),
    onBackground = ColorProvider(light.onBackground, dark.onBackground),
    outline = ColorProvider(light.outline, dark.outline),
  )
