/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025-2026 Elshan Agaev
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

package io.github.calculatorpascaline

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import io.github.calculatorpascaline.core.common.BrowserLocale
import io.github.calculatorpascaline.core.common.WebLanguage
import io.github.calculatorpascaline.core.database.pascalineDatabaseModule
import io.github.calculatorpascaline.core.datastore.UserPreferencesRepository
import io.github.calculatorpascaline.core.datastore.dataStoreModule
import io.github.calculatorpascaline.core.designsystem.LocalWindowSize
import io.github.calculatorpascaline.core.designsystem.theme.LocalNumberTypography
import io.github.calculatorpascaline.core.designsystem.theme.numberTypographyPascaline
import io.github.calculatorpascaline.core.navigation.BodyMassStartRoute
import io.github.calculatorpascaline.core.navigation.CalculatorStartRoute
import io.github.calculatorpascaline.core.navigation.ChangeoverStartRoute
import io.github.calculatorpascaline.core.navigation.ChargingStartRoute
import io.github.calculatorpascaline.core.navigation.ConverterStartRoute
import io.github.calculatorpascaline.core.navigation.DateCalculatorStartRoute
import io.github.calculatorpascaline.core.navigation.ElementsStartRoute
import io.github.calculatorpascaline.core.navigation.FiltersStartRoute
import io.github.calculatorpascaline.core.navigation.GearsStartRoute
import io.github.calculatorpascaline.core.navigation.FinanceStartRoute
import io.github.calculatorpascaline.core.navigation.FunctionsStartRoute
import io.github.calculatorpascaline.core.navigation.HomeStartRoute
import io.github.calculatorpascaline.core.navigation.InferenceStartRoute
import io.github.calculatorpascaline.core.navigation.InflationStartRoute
import io.github.calculatorpascaline.core.navigation.IntegralsStartRoute
import io.github.calculatorpascaline.core.navigation.LAST_TOOL_KEY
import io.github.calculatorpascaline.core.navigation.LogicStartRoute
import io.github.calculatorpascaline.core.navigation.OrbitsStartRoute
import io.github.calculatorpascaline.core.navigation.PlaceStartRoute
import io.github.calculatorpascaline.core.navigation.PricesStartRoute
import io.github.calculatorpascaline.core.navigation.ProbabilityStartRoute
import io.github.calculatorpascaline.core.navigation.ProgrammerStartRoute
import io.github.calculatorpascaline.core.navigation.ProjectileStartRoute
import io.github.calculatorpascaline.core.navigation.RegressionStartRoute
import io.github.calculatorpascaline.core.navigation.ResistorsStartRoute
import io.github.calculatorpascaline.core.navigation.SequencesStartRoute
import io.github.calculatorpascaline.core.navigation.StatisticsStartRoute
import io.github.calculatorpascaline.core.navigation.SurfacesStartRoute
import io.github.calculatorpascaline.core.navigation.lastTool
import io.github.calculatorpascaline.core.ui.BackHandler
import io.github.calculatorpascaline.core.ui.datetime.LocalPlatformDateFormatSettings
import io.github.calculatorpascaline.core.ui.datetime.PlatformDateFormatSettings
import io.github.calculatorpascaline.feature.bodymass.bodyMassModule
import io.github.calculatorpascaline.feature.calculator.calculatorModule
import io.github.calculatorpascaline.feature.converter.converterModule
import io.github.calculatorpascaline.feature.converter.navigation.UnitFromRoute
import io.github.calculatorpascaline.feature.converter.navigation.UnitToRoute
import io.github.calculatorpascaline.feature.datecalculator.dateCalculatorModule
import io.github.calculatorpascaline.feature.settings.settingsModule
import io.github.calculatorpascaline.shared.generated.resources.Res
import io.github.calculatorpascaline.shared.generated.resources.noto_sans
import io.github.calculatorpascaline.shared.generated.resources.noto_sans_devanagari
import io.github.calculatorpascaline.shared.generated.resources.noto_sans_math
import io.github.calculatorpascaline.shared.generated.resources.noto_sans_sc
import io.github.sadellie.themmo.Themmo
import kotlinx.browser.document
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont
import org.koin.compose.koinInject
import org.koin.core.context.startKoin

@OptIn(
  ExperimentalComposeUiApi::class,
  ExperimentalMaterial3WindowSizeClassApi::class,
  ExperimentalMaterial3ExpressiveApi::class,
)
fun main() {
  // Before anything reads the language
  WebLanguage.apply()
  initKoin()
  ComposeViewport(document.body!!) {
    CompositionLocalProvider(
      LocalWindowSize provides rememberWindowSizeClass(),
      LocalNumberTypography provides numberTypographyPascaline(),
      LocalPlatformDateFormatSettings provides
        PlatformDateFormatSettings(
          is24Hour = BrowserLocale.is24Hour(),
          timeZone = TimeZone.currentSystemDefault(),
          locale = Locale.current,
        ),
    ) {
      WithFallbackFonts { App() }
    }
  }
}

/**
 * The browser has no fonts to fall back on: characters missing from the default font, like
 * subscripts, maths symbols, Hindi and Chinese, show as boxes. Preloaded fonts fill them in.
 *
 * Noto fonts (SIL Open Font License, resources/NotoFonts-OFL.txt) from
 * github.com/notofonts/notofonts.github.io and github.com/notofonts/noto-cjk. Math and Chinese are
 * cut down to what the app shows, WebFontsTest says when a translation needs them cut again:
 * ```
 * pyftsubset NotoSansMath-Regular.ttf --layout-features='*' --output-file=noto_sans_math.ttf  *   --unicodes=2190-23FF,25A0-25FF,27C0-27FF,2980-2BFF
 * pyftsubset NotoSansSC-Regular.otf --layout-features='*' --output-file=noto_sans_sc.otf  *   --text-file=<characters of values-zh-rCN/strings.xml and of files/place/cities.tsv> --unicodes=3000-303F,FF00-FF65
 * ```
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun WithFallbackFonts(content: @Composable () -> Unit) {
  val fonts =
    listOf(
      preloadFont(Res.font.noto_sans).value,
      preloadFont(Res.font.noto_sans_math).value,
      preloadFont(Res.font.noto_sans_devanagari).value,
      preloadFont(Res.font.noto_sans_sc).value,
    )
  val fontFamilyResolver = LocalFontFamilyResolver.current
  var fontsReady by remember { mutableStateOf(false) }
  LaunchedEffect(fonts) {
    if (fonts.any { it == null }) return@LaunchedEffect
    // One family each: in a family only one font of a weight is used
    fonts.filterNotNull().forEach { fontFamilyResolver.preload(FontFamily(it)) }
    fontsReady = true
  }
  if (fontsReady) content()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun App() {
  // The settings saved in the browser, like on Android the app waits for them before showing
  val userPrefsRepository = koinInject<UserPreferencesRepository>()
  val appPreferences = userPrefsRepository.appPrefs.collectAsState(null).value ?: return
  // The tool that was open last opens again, the calculator the first time
  val saved =
    produceState<String?>(null, userPrefsRepository) { value = userPrefsRepository.appData(LAST_TOOL_KEY).first() }.value ?: return
  val themmoController = rememberPascalineThemmoController(appPreferences)
  Themmo(themmoController = themmoController) {
    val backStack = rememberNavBackStack(navBackStackConfig, lastTool(saved) ?: CalculatorStartRoute)
    // The browser's back button closes the open screen. Before the content, so a drawer or a
    // search inside it is closed first
    BackHandler(backStack.size > 1) { backStack.removeLastOrNull() }
    MainAppContent(
      backStack = backStack,
      onDrawerItemClick = {},
      themmoController = themmoController,
    )
  }
}

private val navBackStackConfig = SavedStateConfiguration {
  this.serializersModule = SerializersModule {
    polymorphic(NavKey::class) {
      // Every screen the app can start on
      this.subclass(CalculatorStartRoute::class, CalculatorStartRoute.serializer())
      this.subclass(FunctionsStartRoute::class, FunctionsStartRoute.serializer())
      this.subclass(SequencesStartRoute::class, SequencesStartRoute.serializer())
      this.subclass(StatisticsStartRoute::class, StatisticsStartRoute.serializer())
      this.subclass(RegressionStartRoute::class, RegressionStartRoute.serializer())
      this.subclass(InferenceStartRoute::class, InferenceStartRoute.serializer())
      this.subclass(ProbabilityStartRoute::class, ProbabilityStartRoute.serializer())
      this.subclass(ElementsStartRoute::class, ElementsStartRoute.serializer())
      this.subclass(FinanceStartRoute::class, FinanceStartRoute.serializer())
      this.subclass(ProgrammerStartRoute::class, ProgrammerStartRoute.serializer())
      this.subclass(PricesStartRoute::class, PricesStartRoute.serializer())
      this.subclass(BodyMassStartRoute::class, BodyMassStartRoute.serializer())
      this.subclass(HomeStartRoute::class, HomeStartRoute.serializer())
      this.subclass(PlaceStartRoute::class, PlaceStartRoute.serializer())
      this.subclass(ProjectileStartRoute::class, ProjectileStartRoute.serializer())
      this.subclass(OrbitsStartRoute::class, OrbitsStartRoute.serializer())
      this.subclass(GearsStartRoute::class, GearsStartRoute.serializer())
      this.subclass(SurfacesStartRoute::class, SurfacesStartRoute.serializer())
      this.subclass(ChangeoverStartRoute::class, ChangeoverStartRoute.serializer())
      this.subclass(InflationStartRoute::class, InflationStartRoute.serializer())
      this.subclass(IntegralsStartRoute::class, IntegralsStartRoute.serializer())
      this.subclass(FiltersStartRoute::class, FiltersStartRoute.serializer())
      this.subclass(ResistorsStartRoute::class, ResistorsStartRoute.serializer())
      this.subclass(ChargingStartRoute::class, ChargingStartRoute.serializer())
      this.subclass(LogicStartRoute::class, LogicStartRoute.serializer())
      this.subclass(DateCalculatorStartRoute::class, DateCalculatorStartRoute.serializer())
      this.subclass(ConverterStartRoute::class, ConverterStartRoute.serializer())
      this.subclass(UnitFromRoute::class, UnitFromRoute.serializer())
      this.subclass(UnitToRoute::class, UnitToRoute.serializer())
    }
  }
}

private fun initKoin() {
  startKoin {
    modules(
      pascalineDatabaseModule,
      dataStoreModule,
      calculatorModule,
      converterModule,
      bodyMassModule,
      dateCalculatorModule,
      settingsModule,
    )
  }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun rememberWindowSizeClass(): WindowSizeClass {
  val containerSize = LocalWindowInfo.current.containerSize
  val density = LocalDensity.current
  return remember(containerSize, density) {
    with(density) {
      WindowSizeClass.calculateFromSize(
        DpSize(containerSize.width.toDp(), containerSize.height.toDp())
      )
    }
  }
}
