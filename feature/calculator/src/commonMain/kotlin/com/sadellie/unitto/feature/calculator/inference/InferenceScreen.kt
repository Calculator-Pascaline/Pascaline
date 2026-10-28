/*
 * Pascaline is a calculator for Android
 * Copyright (c) 2026 Calculator-Pascaline
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

package com.sadellie.unitto.feature.calculator.inference

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.feature.calculator.datatable.DataTable
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import com.sadellie.unitto.feature.calculator.statistics.StatLine
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.inference_confidence
import unitto.core.common.generated.resources.inference_critical
import unitto.core.common.generated.resources.inference_degrees
import unitto.core.common.generated.resources.inference_deviation
import unitto.core.common.generated.resources.inference_estimate
import unitto.core.common.generated.resources.inference_expected
import unitto.core.common.generated.resources.inference_goodness_of_fit
import unitto.core.common.generated.resources.inference_hypothesis
import unitto.core.common.generated.resources.inference_incomplete
import unitto.core.common.generated.resources.inference_interval
import unitto.core.common.generated.resources.inference_keep
import unitto.core.common.generated.resources.inference_margin
import unitto.core.common.generated.resources.inference_mean
import unitto.core.common.generated.resources.inference_null_value
import unitto.core.common.generated.resources.inference_observed
import unitto.core.common.generated.resources.inference_one_mean_t
import unitto.core.common.generated.resources.inference_one_mean_z
import unitto.core.common.generated.resources.inference_one_proportion
import unitto.core.common.generated.resources.inference_p_value
import unitto.core.common.generated.resources.inference_range
import unitto.core.common.generated.resources.inference_reject
import unitto.core.common.generated.resources.inference_sigma
import unitto.core.common.generated.resources.inference_significance
import unitto.core.common.generated.resources.inference_size
import unitto.core.common.generated.resources.inference_standard_error
import unitto.core.common.generated.resources.inference_statistic
import unitto.core.common.generated.resources.inference_successes
import unitto.core.common.generated.resources.inference_test
import unitto.core.common.generated.resources.inference_title
import unitto.core.common.generated.resources.inference_two_means_t
import unitto.core.common.generated.resources.inference_two_means_z
import unitto.core.common.generated.resources.inference_two_proportions

@Composable
internal fun InferenceRoute(openDrawer: () -> Unit) {
  val viewModel: InferenceViewModel = koinViewModel()
  val format = viewModel.numberFormat.collectAsStateWithLifecycleKMP().value
  if (format == null) {
    EmptyScreen()
    return
  }
  InferenceScreen(openDrawer, viewModel, format)
}

/** Like the Inference app of NumWorks: significance tests and confidence intervals. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InferenceScreen(openDrawer: () -> Unit, viewModel: InferenceViewModel, format: NumberFormat) {
  val kind = viewModel.kind
  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.inference_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    Column(
      modifier = Modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        listOf(true to Res.string.inference_test, false to Res.string.inference_interval).forEachIndexed { index, (isTest, title) ->
          SegmentedButton(
            selected = viewModel.isTest == isTest,
            onClick = { viewModel.selectTest(isTest) },
            shape = SegmentedButtonDefaults.itemShape(index, 2),
            label = { Text(stringResource(title)) },
          )
        }
      }
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InferenceKind.entries
          .filter { viewModel.isTest || it.intervalFields != null }
          .forEach { entry ->
            FilterChip(selected = entry == kind, onClick = { viewModel.select(entry) }, label = { Text(stringResource(entry.title)) })
          }
      }
      if (viewModel.isTest && kind != InferenceKind.GOODNESS_OF_FIT) {
        // H₀: p = p₀ and Hₐ: p ≠ p₀, < or >
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(stringResource(Res.string.inference_hypothesis, kind.hypothesis), style = MaterialTheme.typography.titleMedium)
          Alternative.entries.forEach { alternative ->
            FilterChip(
              selected = viewModel.alternative == alternative,
              onClick = { viewModel.alternative = alternative },
              label = { Text("Hₐ: ${kind.hypothesis.replace("=", alternative.symbol)}") },
            )
          }
        }
      }
      if (viewModel.isTest && kind == InferenceKind.GOODNESS_OF_FIT) {
        val rows = viewModel.goodnessOfFit.rows
        if (rows != null) {
          DataTable(
            modifier = Modifier.fillMaxWidth().heightIn(max = TABLE_HEIGHT),
            headers = listOf(stringResource(Res.string.inference_observed), stringResource(Res.string.inference_expected)),
            rows = rows,
            onCellChange = viewModel.goodnessOfFit::setCell,
            onDeleteRow = viewModel.goodnessOfFit::deleteRow,
            onClear = viewModel.goodnessOfFit::clear,
          )
        }
      } else {
        val fields = (if (viewModel.isTest) kind.testFields else kind.intervalFields).orEmpty()
        // Two samples side by side
        fields.chunked(if (fields.size == 6) 3 else 2).forEach { row ->
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { field ->
              NumberField(
                modifier = Modifier.weight(1f),
                value = viewModel.values[field].orEmpty(),
                onValueChange = { viewModel.values[field] = it },
                label = field.symbol,
                description = stringResource(field.description),
              )
            }
          }
        }
      }
      if (viewModel.isTest) {
        NumberField(
          modifier = Modifier.fillMaxWidth(),
          value = viewModel.significance,
          onValueChange = { viewModel.significance = it },
          label = "α",
          description = stringResource(Res.string.inference_significance),
        )
        TestResultCard(viewModel.test(), kind, viewModel.significance.toNumberOrNull(), format)
      } else {
        NumberField(
          modifier = Modifier.fillMaxWidth(),
          value = viewModel.confidence,
          onValueChange = { viewModel.confidence = it },
          label = "C",
          description = stringResource(Res.string.inference_confidence),
        )
        IntervalResultCard(viewModel.interval(), kind, format)
      }
    }
  }
}

@Composable
private fun TestResultCard(result: TestResult?, kind: InferenceKind, alpha: Double?, format: NumberFormat) {
  ResultCard {
    if (result == null) {
      Incomplete()
      return@ResultCard
    }
    StatLine(stringResource(Res.string.inference_statistic, kind.statisticSymbol), format.format(result.statistic))
    result.degreesOfFreedom?.let { StatLine(stringResource(Res.string.inference_degrees), format.format(it)) }
    StatLine(stringResource(Res.string.inference_p_value), format.format(result.pValue))
    if (alpha != null && alpha > 0 && alpha < 1) {
      val rejects = result.rejects(alpha)
      Text(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        text = stringResource(if (rejects) Res.string.inference_reject else Res.string.inference_keep, format.format(alpha)),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (rejects) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun IntervalResultCard(result: IntervalResult?, kind: InferenceKind, format: NumberFormat) {
  ResultCard {
    if (result == null) {
      Incomplete()
      return@ResultCard
    }
    Text(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      text = "[${format.format(result.low)} ; ${format.format(result.high)}]",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
    )
    StatLine(stringResource(Res.string.inference_estimate), format.format(result.estimate))
    StatLine(stringResource(Res.string.inference_critical, kind.statisticSymbol), format.format(result.criticalValue))
    StatLine(stringResource(Res.string.inference_standard_error), format.format(result.standardError))
    StatLine(stringResource(Res.string.inference_margin), format.format(result.marginOfError))
    result.degreesOfFreedom?.let { StatLine(stringResource(Res.string.inference_degrees), format.format(it)) }
    StatLine(stringResource(Res.string.inference_range), "${format.format(result.low)} ; ${format.format(result.high)}")
  }
}

@Composable
private fun ResultCard(content: @Composable () -> Unit) {
  Column(
    modifier =
      Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 8.dp)
  ) {
    content()
  }
}

@Composable
private fun Incomplete() {
  Text(
    modifier = Modifier.padding(16.dp),
    text = stringResource(Res.string.inference_incomplete),
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun NumberField(
  modifier: Modifier,
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  description: String,
) {
  OutlinedTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    supportingText = { Text(description, maxLines = 2) },
    singleLine = true,
    isError = value.isNotBlank() && value.toNumberOrNull() == null,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
  )
}

/** z, t or χ². */
private val InferenceKind.statisticSymbol: String
  get() =
    when (this) {
      InferenceKind.ONE_MEAN_T,
      InferenceKind.TWO_MEANS_T -> "t"
      InferenceKind.GOODNESS_OF_FIT -> "χ²"
      else -> "z"
    }

private val InferenceKind.title: StringResource
  get() =
    when (this) {
      InferenceKind.ONE_PROPORTION -> Res.string.inference_one_proportion
      InferenceKind.TWO_PROPORTIONS -> Res.string.inference_two_proportions
      InferenceKind.ONE_MEAN_T -> Res.string.inference_one_mean_t
      InferenceKind.ONE_MEAN_Z -> Res.string.inference_one_mean_z
      InferenceKind.TWO_MEANS_T -> Res.string.inference_two_means_t
      InferenceKind.TWO_MEANS_Z -> Res.string.inference_two_means_z
      InferenceKind.GOODNESS_OF_FIT -> Res.string.inference_goodness_of_fit
    }

private val InferenceField.description: StringResource
  get() =
    when (this) {
      InferenceField.P0,
      InferenceField.MU0 -> Res.string.inference_null_value
      InferenceField.SUCCESSES,
      InferenceField.SUCCESSES_1,
      InferenceField.SUCCESSES_2 -> Res.string.inference_successes
      InferenceField.N,
      InferenceField.N_1,
      InferenceField.N_2 -> Res.string.inference_size
      InferenceField.MEAN,
      InferenceField.MEAN_1,
      InferenceField.MEAN_2 -> Res.string.inference_mean
      InferenceField.DEVIATION,
      InferenceField.DEVIATION_1,
      InferenceField.DEVIATION_2 -> Res.string.inference_deviation
      InferenceField.SIGMA,
      InferenceField.SIGMA_1,
      InferenceField.SIGMA_2 -> Res.string.inference_sigma
    }

private val CARD_SHAPE = RoundedCornerShape(20.dp)
private val TABLE_HEIGHT = 360.dp
