/*
 * Unitto is a calculator for Android
 * Copyright (c) 2024-2026 Elshan Agaev
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

package com.sadellie.unitto.feature.bodymass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.designsystem.shapes.Sizes
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.core.ui.TextFieldBox
import com.sadellie.unitto.core.ui.TextFieldRow
import com.sadellie.unitto.feature.bodymass.components.BodyMassGuide
import com.sadellie.unitto.feature.bodymass.components.BodyMassResult
import com.sadellie.unitto.feature.bodymass.components.BodyMassTextField
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.body_mass_height
import unitto.core.common.generated.resources.body_mass_imperial
import unitto.core.common.generated.resources.body_mass_metric
import unitto.core.common.generated.resources.body_mass_title
import unitto.core.common.generated.resources.body_mass_waist
import unitto.core.common.generated.resources.body_mass_weight
import unitto.core.common.generated.resources.unit_centimeter_short
import unitto.core.common.generated.resources.unit_foot_short
import unitto.core.common.generated.resources.unit_inch_short
import unitto.core.common.generated.resources.unit_kilogram_short
import unitto.core.common.generated.resources.unit_pound_short

@Composable
internal fun BodyMassRoute(openDrawer: () -> Unit) {
  val viewModel: BodyMassViewModel = koinViewModel()
  LaunchedEffect(Unit) { viewModel.observeInput() }

  when (val uiState = viewModel.uiState.collectAsStateWithLifecycleKMP().value) {
    UIState.Loading -> EmptyScreen()
    is UIState.Ready ->
      BodyMassScreen(
        uiState = uiState,
        updateIsMetric = viewModel::updateIsMetric,
        openDrawer = openDrawer,
      )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BodyMassScreen(
  uiState: UIState.Ready,
  updateIsMetric: (Boolean) -> Unit,
  openDrawer: () -> Unit,
) {
  val weightShortLabel =
    stringResource(
      if (uiState.isMetric) Res.string.unit_kilogram_short else Res.string.unit_pound_short
    )

  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.body_mass_title)) },
    navigationIcon = { DrawerButton(openDrawer) },
  ) { paddingValues ->
    BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      val isWide = maxWidth >= WIDE_LAYOUT_WIDTH
      val form: @Composable (Modifier) -> Unit = { formModifier ->
        Column(modifier = formModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
          BodyMassInputModeSelector(
            modifier = Modifier.fillMaxWidth(),
            updateIsMetric = updateIsMetric,
            isMetric = uiState.isMetric,
          )
          BodyMassInputBox(
            modifier = Modifier.fillMaxWidth(),
            uiState = uiState,
            weightShortLabel = weightShortLabel,
          )
          BodyMassResult(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.result,
            range = uiState.normalWeightRange,
            rangeSuffix = weightShortLabel,
            waistToHeight = uiState.waistToHeight,
            formatterSymbols = uiState.formatterSymbols,
          )
        }
      }
      val scrolling = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
      if (isWide) {
        Row(modifier = scrolling, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          form(Modifier.weight(1f))
          BodyMassGuide(modifier = Modifier.weight(1f))
        }
      } else {
        Column(modifier = scrolling, verticalArrangement = Arrangement.spacedBy(16.dp)) {
          form(Modifier.fillMaxWidth())
          BodyMassGuide(modifier = Modifier.fillMaxWidth())
        }
      }
    }
  }
}

/** Height and weight fit side by side in metric. Feet and inches take that row in imperial. */
@Composable
private fun BodyMassInputBox(modifier: Modifier, uiState: UIState.Ready, weightShortLabel: String) {
  val height = stringResource(Res.string.body_mass_height)
  val weightLabel = "${stringResource(Res.string.body_mass_weight)}, $weightShortLabel"
  val symbols = uiState.formatterSymbols
  TextFieldBox(modifier = modifier) {
    if (uiState.isMetric) {
      TextFieldRow {
        val centimeter = stringResource(Res.string.unit_centimeter_short)
        BodyMassTextField(Modifier.weight(1f), uiState.height1, "$height, $centimeter", symbols)
        BodyMassTextField(Modifier.weight(1f), uiState.weight, weightLabel, symbols)
      }
    } else {
      TextFieldRow {
        val foot = stringResource(Res.string.unit_foot_short)
        val inch = stringResource(Res.string.unit_inch_short)
        BodyMassTextField(Modifier.weight(1f), uiState.height1, "$height, $foot", symbols)
        BodyMassTextField(Modifier.weight(1f), uiState.height2, "$height, $inch", symbols)
      }
      TextFieldRow {
        BodyMassTextField(Modifier.fillMaxWidth(), uiState.weight, weightLabel, symbols)
      }
    }
    TextFieldRow {
      val unit = if (uiState.isMetric) Res.string.unit_centimeter_short else Res.string.unit_inch_short
      BodyMassTextField(
        modifier = Modifier.fillMaxWidth(),
        state = uiState.waist,
        label = "${stringResource(Res.string.body_mass_waist)}, ${stringResource(unit)}",
        formatterSymbols = symbols,
        imeAction = ImeAction.Done,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BodyMassInputModeSelector(
  modifier: Modifier,
  updateIsMetric: (Boolean) -> Unit,
  isMetric: Boolean,
) {
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Sizes.large)) {
    ToggleButton(
      onCheckedChange = { updateIsMetric(true) },
      checked = isMetric,
      modifier = Modifier.weight(1f),
    ) {
      Text(stringResource(Res.string.body_mass_metric))
    }
    ToggleButton(
      onCheckedChange = { updateIsMetric(false) },
      checked = !isMetric,
      modifier = Modifier.weight(1f),
    ) {
      Text(stringResource(Res.string.body_mass_imperial))
    }
  }
}

@Preview
@Composable
fun PreviewBodyMassScreen() {
  BodyMassScreen(
    uiState =
      UIState.Ready(
        isMetric = false,
        height1 = remember { TextFieldState() },
        height2 = remember { TextFieldState() },
        weight = remember { TextFieldState() },
        waist = remember { TextFieldState() },
        normalWeightRange = KBigDecimal(30.0) to KBigDecimal(50.0),
        result = KBigDecimal(18.5),
        waistToHeight = KBigDecimal.ZERO,
        formatterSymbols = FormatterSymbols(Token.SPACE, Token.PERIOD, false),
      ),
    updateIsMetric = {},
    openDrawer = {},
  )
}

private val WIDE_LAYOUT_WIDTH = 720.dp
