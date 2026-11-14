/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2026 Elshan Agaev
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

package io.github.calculatorpascaline.feature.settings.calculator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.calculatorpascaline.core.common.FormatterSymbols
import io.github.calculatorpascaline.core.common.OutputFormat
import io.github.calculatorpascaline.core.common.Token
import io.github.calculatorpascaline.core.common.collectAsStateWithLifecycleKMP
import io.github.calculatorpascaline.core.datastore.CalculatorPreferences
import io.github.calculatorpascaline.core.designsystem.icons.iconpack.Fraction
import io.github.calculatorpascaline.core.designsystem.icons.iconpack.IconPack
import io.github.calculatorpascaline.core.designsystem.icons.symbols.AddRowBelow
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Symbols
import io.github.calculatorpascaline.core.designsystem.shapes.Sizes
import io.github.calculatorpascaline.core.ui.EmptyScreen
import io.github.calculatorpascaline.core.ui.ListItemExpressive
import io.github.calculatorpascaline.core.ui.ListItemExpressiveDefaults
import io.github.calculatorpascaline.core.ui.NavigateUpButton
import io.github.calculatorpascaline.core.ui.ScaffoldWithLargeTopBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.calculator_title
import pascaline.core.common.generated.resources.settings_constant_calculation
import pascaline.core.common.generated.resources.settings_constant_calculation_support
import pascaline.core.common.generated.resources.settings_fractional_output
import pascaline.core.common.generated.resources.settings_fractional_output_support

@Composable
internal fun CalculatorSettingsRoute(navigateUpAction: () -> Unit) {
  val viewModel: CalculatorSettingsViewModel = koinViewModel()
  when (val prefs = viewModel.prefs.collectAsStateWithLifecycleKMP().value) {
    null -> EmptyScreen()
    else -> {
      CalculatorSettingsScreen(
        prefs = prefs,
        navigateUpAction = navigateUpAction,
        updateFractionalOutput = viewModel::updateFractionalOutput,
        updateConstantCalculation = viewModel::updateConstantCalculation,
      )
    }
  }
}

@Composable
private fun CalculatorSettingsScreen(
  prefs: CalculatorPreferences,
  navigateUpAction: () -> Unit,
  updateFractionalOutput: (Boolean) -> Unit,
  updateConstantCalculation: (Boolean) -> Unit,
) {
  ScaffoldWithLargeTopBar(
    title = stringResource(Res.string.calculator_title),
    navigationIcon = { NavigateUpButton(navigateUpAction) },
  ) { padding ->
    Column(
      modifier =
        Modifier.padding(padding)
          .padding(start = Sizes.large, end = Sizes.large, bottom = Sizes.large),
      verticalArrangement = ListItemExpressiveDefaults.ListArrangement,
    ) {
      ListItemExpressive(
        headlineText = stringResource(Res.string.settings_fractional_output),
        icon = IconPack.Fraction,
        supportingText = stringResource(Res.string.settings_fractional_output_support),
        switchState = prefs.fractionalOutput,
        onSwitchChange = updateFractionalOutput,
        shape = ListItemExpressiveDefaults.firstShape,
      )

      ListItemExpressive(
        headlineText = stringResource(Res.string.settings_constant_calculation),
        icon = Symbols.AddRowBelow,
        supportingText = stringResource(Res.string.settings_constant_calculation_support),
        switchState = prefs.constantCalculation,
        onSwitchChange = updateConstantCalculation,
        shape = ListItemExpressiveDefaults.lastShape,
      )
    }
  }
}

@Preview
@Composable
private fun PreviewCalculatorSettingsScreenStandard() {
  var prefs by remember {
    mutableStateOf(
      CalculatorPreferences(
        radianMode = true,
        formatterSymbols = FormatterSymbols(Token.SPACE, Token.PERIOD, false),
        additionalButtons = false,
        inverseMode = false,
        partialHistoryView = false,
        steppedPartialHistoryView = false,
        initialPartialHistoryView = false,
        openHistoryViewButton = false,
        precision = 3,
        outputFormat = OutputFormat.PLAIN,
        fractionalOutput = true,
        constantCalculation = false,
      )
    )
  }
  CalculatorSettingsScreen(
    prefs = prefs,
    navigateUpAction = {},
    updateFractionalOutput = { prefs = prefs.copy(fractionalOutput = it) },
    updateConstantCalculation = { prefs = prefs.copy(constantCalculation = it) },
  )
}
