/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2026 Elshan Agaev
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

package io.github.calculatorpascaline.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.calculatorpascaline.core.common.Config
import io.github.calculatorpascaline.core.common.collectAsStateWithLifecycleKMP
import io.github.calculatorpascaline.core.designsystem.LocalWindowSize
import io.github.calculatorpascaline.core.designsystem.icons.symbols.BacklightHigh
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Calculate
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Code
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Copyright
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Favorite
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Info
import io.github.calculatorpascaline.core.designsystem.icons.symbols.LockScreen
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Palette
import io.github.calculatorpascaline.core.designsystem.icons.symbols.RateReview
import io.github.calculatorpascaline.core.designsystem.icons.symbols.RestorePage
import io.github.calculatorpascaline.core.designsystem.icons.symbols.SwapHoriz
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Keyboard
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Symbols
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Vibration
import io.github.calculatorpascaline.core.designsystem.icons.symbols._123
import io.github.calculatorpascaline.core.designsystem.shapes.Sizes
import io.github.calculatorpascaline.core.navigation.Route
import io.github.calculatorpascaline.core.ui.DrawerButton
import io.github.calculatorpascaline.core.ui.EmptyScreen
import io.github.calculatorpascaline.core.ui.ListHeader
import io.github.calculatorpascaline.core.ui.ListItemExpressive
import io.github.calculatorpascaline.core.ui.ListItemExpressiveDefaults
import io.github.calculatorpascaline.core.ui.LinkOpener
import io.github.calculatorpascaline.core.ui.LocalPlatform
import io.github.calculatorpascaline.core.ui.Platform
import io.github.calculatorpascaline.core.ui.ScaffoldWithLargeTopBar
import io.github.calculatorpascaline.core.ui.rememberLinkOpener
import io.github.calculatorpascaline.feature.settings.navigation.BackupRoute
import io.github.calculatorpascaline.feature.settings.navigation.CalculatorSettingsRoute
import io.github.calculatorpascaline.feature.settings.navigation.ConverterSettingsRoute
import io.github.calculatorpascaline.feature.settings.navigation.DisplayRoute
import io.github.calculatorpascaline.feature.settings.navigation.FormattingRoute
import io.github.calculatorpascaline.feature.settings.navigation.ThirdPartyRoute
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.calculator_title
import pascaline.core.common.generated.resources.converter_title
import pascaline.core.common.generated.resources.settings_about
import pascaline.core.common.generated.resources.settings_apps
import pascaline.core.common.generated.resources.settings_backup
import pascaline.core.common.generated.resources.settings_backup_support
import pascaline.core.common.generated.resources.settings_based_on_unitto
import pascaline.core.common.generated.resources.settings_calculator_support
import pascaline.core.common.generated.resources.settings_converter_support
import pascaline.core.common.generated.resources.settings_display
import pascaline.core.common.generated.resources.settings_display_support
import pascaline.core.common.generated.resources.settings_formatting
import pascaline.core.common.generated.resources.settings_formatting_support
import pascaline.core.common.generated.resources.settings_general
import pascaline.core.common.generated.resources.settings_keep_screen_on
import pascaline.core.common.generated.resources.settings_keep_screen_on_support
import pascaline.core.common.generated.resources.settings_phone
import pascaline.core.common.generated.resources.settings_report_issue
import pascaline.core.common.generated.resources.settings_report_issue_support
import pascaline.core.common.generated.resources.settings_show_on_lock_screen
import pascaline.core.common.generated.resources.settings_show_on_lock_screen_support
import pascaline.core.common.generated.resources.settings_third_party_licenses
import pascaline.core.common.generated.resources.settings_title
import pascaline.core.common.generated.resources.settings_version_name
import pascaline.core.common.generated.resources.settings_key_animation
import pascaline.core.common.generated.resources.settings_key_animation_support
import pascaline.core.common.generated.resources.settings_vibrations
import pascaline.core.common.generated.resources.settings_vibrations_support
import pascaline.core.common.generated.resources.settings_view_source_code

@Composable
internal fun SettingsRoute(openDrawer: () -> Unit, navControllerAction: (route: Route) -> Unit) {
  val viewModel: SettingsViewModel = koinViewModel()

  when (val uiState: SettingsUIState = viewModel.uiState.collectAsStateWithLifecycleKMP().value) {
    SettingsUIState.Loading -> EmptyScreen()
    is SettingsUIState.Ready ->
      SettingsScreen(
        uiState = uiState,
        openDrawer = openDrawer,
        navControllerAction = navControllerAction,
        updateVibrations = viewModel::updateVibrations,
        updateKeyAnimation = viewModel::updateKeyAnimation,
        updateKeepScreenOn = viewModel::updateEnableKeepScreenOn,
        updateShowOnLockScreen = viewModel::updateShowOnLockScreen,
      )
  }
}

@Composable
private fun SettingsScreen(
  uiState: SettingsUIState.Ready,
  openDrawer: () -> Unit,
  navControllerAction: (Route) -> Unit,
  updateVibrations: (Boolean) -> Unit,
  updateKeyAnimation: (Boolean) -> Unit,
  updateKeepScreenOn: (Boolean) -> Unit,
  updateShowOnLockScreen: (Boolean) -> Unit,
) {
  ScaffoldWithLargeTopBar(
    title = stringResource(Res.string.settings_title),
    navigationIcon = {
      if (LocalWindowSize.current.widthSizeClass != WindowWidthSizeClass.Expanded) {
        DrawerButton(openDrawer)
      }
    },
  ) { padding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(padding)
          .padding(start = Sizes.large, end = Sizes.large, bottom = Sizes.large),
      verticalArrangement = ListItemExpressiveDefaults.ListArrangement,
    ) {
      val linkOpener = rememberLinkOpener()
      GeneralSettings(navControllerAction)
      AppSettings(navControllerAction)
      DeviceSettings(uiState, navControllerAction, updateVibrations, updateKeyAnimation, updateKeepScreenOn, updateShowOnLockScreen)
      AboutSettings(linkOpener, navControllerAction)
    }
  }
}

/** Display and formatting. */
@Composable
private fun GeneralSettings(navControllerAction: (Route) -> Unit) {
  ListHeader(stringResource(Res.string.settings_general))
  ListItemExpressive(
    icon = Symbols.Palette,
    headlineText = stringResource(Res.string.settings_display),
    supportingText = stringResource(Res.string.settings_display_support),
    onClick = { navControllerAction(DisplayRoute) },
    shape = ListItemExpressiveDefaults.firstShape,
  )
  ListItemExpressive(
    icon = Symbols._123,
    headlineText = stringResource(Res.string.settings_formatting),
    supportingText = stringResource(Res.string.settings_formatting_support),
    onClick = { navControllerAction(FormattingRoute) },
    shape = ListItemExpressiveDefaults.lastShape,
  )
}

/** Settings of the calculator and the converter. */
@Composable
private fun AppSettings(navControllerAction: (Route) -> Unit) {
  ListHeader(stringResource(Res.string.settings_apps))
  ListItemExpressive(
    icon = Symbols.Calculate,
    headlineText = stringResource(Res.string.calculator_title),
    supportingText = stringResource(Res.string.settings_calculator_support),
    onClick = { navControllerAction(CalculatorSettingsRoute) },
    shape = ListItemExpressiveDefaults.firstShape,
  )
  ListItemExpressive(
    icon = Symbols.SwapHoriz,
    headlineText = stringResource(Res.string.converter_title),
    supportingText = stringResource(Res.string.settings_converter_support),
    onClick = { navControllerAction(ConverterSettingsRoute) },
    shape = ListItemExpressiveDefaults.lastShape,
  )
}

/** Vibrations, the screen and the lock screen on Android, backup everywhere. */
@Composable
private fun DeviceSettings(
  uiState: SettingsUIState.Ready,
  navControllerAction: (Route) -> Unit,
  updateVibrations: (Boolean) -> Unit,
  updateKeyAnimation: (Boolean) -> Unit,
  updateKeepScreenOn: (Boolean) -> Unit,
  updateShowOnLockScreen: (Boolean) -> Unit,
) {
  val isAndroid = LocalPlatform.current == Platform.Android
  // Vibrations and the screen only exist in the Android app
  if (LocalPlatform.current == Platform.Android) {
    ListHeader(stringResource(Res.string.settings_phone))
    ListItemExpressive(
      icon = Symbols.Vibration,
      headlineText = stringResource(Res.string.settings_vibrations),
      supportingText = stringResource(Res.string.settings_vibrations_support),
      switchState = uiState.enableVibrations,
      onSwitchChange = updateVibrations,
      shape = ListItemExpressiveDefaults.firstShape,
    )
    ListItemExpressive(
      icon = Symbols.BacklightHigh,
      headlineText = stringResource(Res.string.settings_keep_screen_on),
      supportingText = stringResource(Res.string.settings_keep_screen_on_support),
      switchState = uiState.enableKeepScreenOn,
      onSwitchChange = updateKeepScreenOn,
      shape = ListItemExpressiveDefaults.middleShape,
    )
    ListItemExpressive(
      icon = Symbols.LockScreen,
      headlineText = stringResource(Res.string.settings_show_on_lock_screen),
      supportingText = stringResource(Res.string.settings_show_on_lock_screen_support),
      switchState = uiState.showOnLockScreen,
      onSwitchChange = updateShowOnLockScreen,
      shape = ListItemExpressiveDefaults.middleShape,
    )
  }
  ListItemExpressive(
    icon = Symbols.Keyboard,
    headlineText = stringResource(Res.string.settings_key_animation),
    supportingText = stringResource(Res.string.settings_key_animation_support),
    switchState = uiState.enableKeyAnimation,
    onSwitchChange = updateKeyAnimation,
    shape = if (isAndroid) ListItemExpressiveDefaults.middleShape else ListItemExpressiveDefaults.firstShape,
  )
  // The web app has its own backup file, on its own after the apps
  ListItemExpressive(
    icon = Symbols.RestorePage,
    headlineText = stringResource(Res.string.settings_backup),
    supportingText = stringResource(Res.string.settings_backup_support),
    onClick = { navControllerAction(BackupRoute) },
    shape = ListItemExpressiveDefaults.lastShape,
  )
}

/** Feedback, source code, credits, licenses and the version. */
@Composable
private fun AboutSettings(linkOpener: LinkOpener, navControllerAction: (Route) -> Unit) {
  // Feedback, source code for the GPL, credits and licenses of the libraries
  ListHeader(stringResource(Res.string.settings_about))
  ListItemExpressive(
    icon = Symbols.RateReview,
    headlineText = stringResource(Res.string.settings_report_issue),
    supportingText = stringResource(Res.string.settings_report_issue_support),
    onClick = { linkOpener.launch(REPORT_ISSUE_LINK) },
    shape = ListItemExpressiveDefaults.firstShape,
  )
  ListItemExpressive(
    icon = Symbols.Code,
    headlineText = stringResource(Res.string.settings_view_source_code),
    onClick = { linkOpener.launch(Config.SOURCE_CODE_LINK) },
    shape = ListItemExpressiveDefaults.middleShape,
  )
  ListItemExpressive(
    icon = Symbols.Favorite,
    headlineText = stringResource(Res.string.settings_based_on_unitto),
    onClick = { linkOpener.launch(Config.UPSTREAM_LINK) },
    shape = ListItemExpressiveDefaults.middleShape,
  )
  ListItemExpressive(
    icon = Symbols.Copyright,
    headlineText = stringResource(Res.string.settings_third_party_licenses),
    onClick = { navControllerAction(ThirdPartyRoute) },
    shape = ListItemExpressiveDefaults.middleShape,
  )
  ListItemExpressive(
    icon = Symbols.Info,
    headlineText = stringResource(Res.string.settings_version_name),
    supportingText = Config.VERSION_NAME,
    onClick = null,
    shape = ListItemExpressiveDefaults.lastShape,
  )
}

@Preview
@Composable
private fun PreviewSettingsScreen() {
  var uiState by remember {
    mutableStateOf(
      SettingsUIState.Ready(
        enableVibrations = false,
        enableKeepScreenOn = false,
        showOnLockScreen = false,
        enableKeyAnimation = true,
      )
    )
  }

  SettingsScreen(
    uiState = uiState,
    openDrawer = {},
    navControllerAction = {},
    updateVibrations = { uiState = uiState.copy(enableVibrations = it) },
    updateKeyAnimation = { uiState = uiState.copy(enableKeyAnimation = it) },
    updateKeepScreenOn = { uiState = uiState.copy(enableKeepScreenOn = it) },
    updateShowOnLockScreen = { uiState = uiState.copy(showOnLockScreen = it) },
  )
}

private const val REPORT_ISSUE_LINK = "https://github.com/Calculator-Pascaline/Pascaline/issues/new"
