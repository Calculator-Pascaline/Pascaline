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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.designsystem.icons.symbols.KeyboardArrowDown
import com.sadellie.unitto.core.designsystem.icons.symbols.KeyboardArrowUp
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.feature.calculator.datatable.DataTable
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.datatable.column
import com.sadellie.unitto.feature.calculator.physics.LiveCard
import com.sadellie.unitto.feature.calculator.physics.PhysicsScreen
import com.sadellie.unitto.feature.calculator.physics.rememberFractional
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import com.sadellie.unitto.feature.calculator.projectile.Answer
import com.sadellie.unitto.feature.calculator.projectile.Card
import com.sadellie.unitto.feature.calculator.projectile.Condition
import com.sadellie.unitto.feature.calculator.projectile.ConditionRow
import com.sadellie.unitto.feature.calculator.statistics.StatLine
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.inference_ask_average
import unitto.core.common.generated.resources.inference_ask_counts
import unitto.core.common.generated.resources.inference_ask_percentage
import unitto.core.common.generated.resources.inference_ask_two_averages
import unitto.core.common.generated.resources.inference_ask_two_percentages
import unitto.core.common.generated.resources.inference_chance
import unitto.core.common.generated.resources.inference_confidence
import unitto.core.common.generated.resources.inference_critical
import unitto.core.common.generated.resources.inference_degrees
import unitto.core.common.generated.resources.inference_deviation
import unitto.core.common.generated.resources.inference_estimate
import unitto.core.common.generated.resources.inference_expected
import unitto.core.common.generated.resources.inference_goodness_of_fit
import unitto.core.common.generated.resources.inference_group_1
import unitto.core.common.generated.resources.inference_group_2
import unitto.core.common.generated.resources.inference_hint_overlap
import unitto.core.common.generated.resources.inference_hint_tails
import unitto.core.common.generated.resources.inference_how_average
import unitto.core.common.generated.resources.inference_how_percentage
import unitto.core.common.generated.resources.inference_how_two_averages
import unitto.core.common.generated.resources.inference_how_two_percentages
import unitto.core.common.generated.resources.inference_if_null
import unitto.core.common.generated.resources.inference_incomplete
import unitto.core.common.generated.resources.inference_interval
import unitto.core.common.generated.resources.inference_interval_sentence
import unitto.core.common.generated.resources.inference_keep
import unitto.core.common.generated.resources.inference_known_sigma
import unitto.core.common.generated.resources.inference_margin
import unitto.core.common.generated.resources.inference_mean
import unitto.core.common.generated.resources.inference_null_value
import unitto.core.common.generated.resources.inference_observed
import unitto.core.common.generated.resources.inference_one_mean_t
import unitto.core.common.generated.resources.inference_one_mean_z
import unitto.core.common.generated.resources.inference_one_proportion
import unitto.core.common.generated.resources.inference_p_value
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
import unitto.core.common.generated.resources.inference_verdict_real
import unitto.core.common.generated.resources.inference_verdict_unclear
import unitto.core.common.generated.resources.inference_your_data
import unitto.core.common.generated.resources.inference_your_sample

private val SIGNIFICANCE = Condition("α", "", Res.string.inference_significance, 0.0..0.2, 0.01)
private val CONFIDENCE = Condition("C", "", Res.string.inference_confidence, 0.5..0.99, 0.01)

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
/** A question in plain words, the two curves that answer it and a verdict anyone can read. */
@Composable
private fun InferenceScreen(openDrawer: () -> Unit, viewModel: InferenceViewModel, format: NumberFormat) {
  val fractional = rememberFractional()
  PhysicsScreen(
    title = Res.string.inference_title,
    openDrawer = openDrawer,
    live = {
      ModeSwitch(viewModel)
      QuestionCard(viewModel)
      if (viewModel.isTest) TestCard(viewModel, format) else IntervalCard(viewModel, format)
    },
    settings = {},
    explanation = { DataCard(viewModel, fractional) },
  )
}

@Composable
private fun ModeSwitch(viewModel: InferenceViewModel) {
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
}

/** The question asked. Tap it to see the others, the technical name stays under it. */
@Composable
private fun QuestionCard(viewModel: InferenceViewModel) {
  var isOpen by rememberSaveable { mutableStateOf(false) }
  val selected = viewModel.question
  Column(modifier = Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer)) {
    Row(
      modifier = Modifier.fillMaxWidth().clickable { isOpen = !isOpen }.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column(Modifier.weight(1f)) {
        Text(stringResource(selected.ask(viewModel.isTest)), style = MaterialTheme.typography.titleMedium)
        Text(
          stringResource(viewModel.kind.title),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Icon(if (isOpen) Symbols.KeyboardArrowUp else Symbols.KeyboardArrowDown, contentDescription = null)
    }
    AnimatedVisibility(isOpen) {
      Column {
        InferenceQuestion.entries
          .filter { it != selected && (viewModel.isTest || it != InferenceQuestion.COUNTS) }
          .forEach { question ->
            Text(
              text = stringResource(question.ask(viewModel.isTest)),
              modifier =
                Modifier.fillMaxWidth()
                  .clickable {
                    viewModel.ask(question)
                    isOpen = false
                  }
                  .padding(horizontal = 16.dp, vertical = 12.dp),
              style = MaterialTheme.typography.bodyLarge,
            )
          }
      }
    }
    if (selected.knownSigma != null) {
      Row(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.inference_known_sigma), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = viewModel.knowsSigma, onCheckedChange = viewModel::selectKnowsSigma)
      }
    }
  }
}

private fun InferenceQuestion.ask(isTest: Boolean): StringResource =
  when (this) {
    InferenceQuestion.PERCENTAGE -> if (isTest) Res.string.inference_ask_percentage else Res.string.inference_how_percentage
    InferenceQuestion.TWO_PERCENTAGES -> if (isTest) Res.string.inference_ask_two_percentages else Res.string.inference_how_two_percentages
    InferenceQuestion.AVERAGE -> if (isTest) Res.string.inference_ask_average else Res.string.inference_how_average
    InferenceQuestion.TWO_AVERAGES -> if (isTest) Res.string.inference_ask_two_averages else Res.string.inference_how_two_averages
    InferenceQuestion.COUNTS -> Res.string.inference_ask_counts
  }

private val InferenceKind.isTwoGroups: Boolean
  get() = fieldsPerRow(testFields) == TWO_SAMPLE_ROW || this == InferenceKind.TWO_PROPORTIONS

private fun fieldsPerRow(fields: List<InferenceField>) = if (fields.size == TWO_SAMPLE_FIELDS) TWO_SAMPLE_ROW else ONE_SAMPLE_ROW

/** The curves, the verdict and the numbers behind it. */
@Composable
private fun TestCard(viewModel: InferenceViewModel, format: NumberFormat) {
  val kind = viewModel.kind
  val result = viewModel.test()
  if (result == null) {
    Incomplete()
    return
  }
  LiveCard {
    if (kind == InferenceKind.GOODNESS_OF_FIT) {
      val rows = viewModel.goodnessOfFit.rows.orEmpty()
      CountBars(rows.column(0).filterNotNull(), rows.column(1).filterNotNull())
      ChartLegend(stringResource(Res.string.inference_observed), stringResource(Res.string.inference_expected))
    } else {
      TwoCurves(viewModel, kind)
    }
    val alpha = viewModel.significance.toNumberOrNull()
    if (alpha != null && alpha > 0 && alpha < 1) Verdict(result, alpha, format)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Answer(Modifier.weight(1f), kind.statisticSymbol, format.format(result.statistic))
      Answer(Modifier.weight(1f), Res.string.inference_p_value, format.format(result.pValue))
      result.degreesOfFreedom?.let { Answer(Modifier.weight(1f), "df", format.format(it)) }
    }
  }
}

@Composable
private fun TwoCurves(viewModel: InferenceViewModel, kind: InferenceKind) {
  val (first, second) = viewModel.bells() ?: return
  val isTwoGroups = kind.isTwoGroups
  BellChart(first, second, if (isTwoGroups) BellShade.Overlap else BellShade.Tails(second.center, viewModel.alternative))
  ChartLegend(
    stringResource(if (isTwoGroups) Res.string.inference_group_1 else Res.string.inference_if_null),
    stringResource(if (isTwoGroups) Res.string.inference_group_2 else Res.string.inference_your_sample),
  )
  Text(
    stringResource(if (isTwoGroups) Res.string.inference_hint_overlap else Res.string.inference_hint_tails),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

/** In plain words first, then the words of statistics. */
@Composable
private fun Verdict(result: TestResult, alpha: Double, format: NumberFormat) {
  val rejects = result.rejects(alpha)
  val colors = MaterialTheme.colorScheme
  val text = if (rejects) colors.onTertiaryContainer else colors.onSurface
  Column(
    modifier =
      Modifier.fillMaxWidth().clip(VERDICT_SHAPE).background(if (rejects) colors.tertiaryContainer else colors.surfaceContainerHighest).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(
      stringResource(if (rejects) Res.string.inference_verdict_real else Res.string.inference_verdict_unclear),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = text,
    )
    Text(stringResource(Res.string.inference_chance, format.format(result.pValue * PERCENT)), style = MaterialTheme.typography.bodyMedium, color = text)
    Text(
      stringResource(if (rejects) Res.string.inference_reject else Res.string.inference_keep, format.format(alpha)),
      style = MaterialTheme.typography.bodySmall,
      color = text,
    )
  }
}

@Composable
private fun IntervalCard(viewModel: InferenceViewModel, format: NumberFormat) {
  val result = viewModel.interval()
  if (result == null) {
    Incomplete()
    return
  }
  val low = format.format(result.low)
  val high = format.format(result.high)
  LiveCard {
    BellChart(Bell(result.estimate, result.standardError), null, BellShade.Middle(result.low, result.high))
    Text(
      "[$low ; $high]",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
    )
    val level = viewModel.confidence.toNumberOrNull() ?: 0.0
    Text(stringResource(Res.string.inference_interval_sentence, format.format(level * PERCENT), low, high), style = MaterialTheme.typography.bodyMedium)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Answer(Modifier.weight(1f), Res.string.inference_estimate, format.format(result.estimate))
      Answer(Modifier.weight(1f), Res.string.inference_margin, "± ${format.format(result.marginOfError)}")
    }
    Column {
      StatLine(stringResource(Res.string.inference_critical, viewModel.kind.statisticSymbol), format.format(result.criticalValue))
      StatLine(stringResource(Res.string.inference_standard_error), format.format(result.standardError))
      result.degreesOfFreedom?.let { StatLine(stringResource(Res.string.inference_degrees), format.format(it)) }
    }
  }
}

/** What was measured and how sure the answer has to be. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DataCard(viewModel: InferenceViewModel, fractional: String) {
  val kind = viewModel.kind
  Card(stringResource(Res.string.inference_your_data)) {
    if (viewModel.isTest && kind != InferenceKind.GOODNESS_OF_FIT) {
      FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
      viewModel.goodnessOfFit.rows?.let { rows ->
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
      fields.chunked(fieldsPerRow(fields)).forEach { row ->
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
      ConditionRow(SIGNIFICANCE, viewModel.significance, fractional) { viewModel.significance = it }
    } else {
      ConditionRow(CONFIDENCE, viewModel.confidence, fractional) { viewModel.confidence = it }
    }
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

private const val PERCENT = 100
private val VERDICT_SHAPE = RoundedCornerShape(16.dp)
private val CARD_SHAPE = RoundedCornerShape(20.dp)
private val TABLE_HEIGHT = 360.dp

/** Two samples have 6 fields, 3 a row: one row each */
private const val TWO_SAMPLE_FIELDS = 6
private const val TWO_SAMPLE_ROW = 3
private const val ONE_SAMPLE_ROW = 2
