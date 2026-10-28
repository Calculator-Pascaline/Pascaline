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

package com.sadellie.unitto.feature.calculator.probability

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.designsystem.icons.symbols.Help
import com.sadellie.unitto.core.designsystem.icons.symbols.KeyboardArrowDown
import com.sadellie.unitto.core.designsystem.icons.symbols.KeyboardArrowUp
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import kotlin.math.abs
import kotlin.math.sqrt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.probability_baskets_average
import unitto.core.common.generated.resources.probability_curve_center
import unitto.core.common.generated.resources.probability_deviation_name
import unitto.core.common.generated.resources.probability_example_sports
import unitto.core.common.generated.resources.probability_flips_column
import unitto.core.common.generated.resources.probability_from_probabilities
import unitto.core.common.generated.resources.probability_heads_average
import unitto.core.common.generated.resources.probability_how_expected
import unitto.core.common.generated.resources.probability_how_variance
import unitto.core.common.generated.resources.probability_mean_label
import unitto.core.common.generated.resources.probability_mean_name
import unitto.core.common.generated.resources.probability_moments
import unitto.core.common.generated.resources.probability_moments_how
import unitto.core.common.generated.resources.probability_one_flip_column
import unitto.core.common.generated.resources.probability_one_flip_table
import unitto.core.common.generated.resources.probability_one_person
import unitto.core.common.generated.resources.probability_one_throw_column
import unitto.core.common.generated.resources.probability_one_throw_table
import unitto.core.common.generated.resources.probability_people
import unitto.core.common.generated.resources.probability_quick_flips
import unitto.core.common.generated.resources.probability_quick_throws
import unitto.core.common.generated.resources.probability_sample_of
import unitto.core.common.generated.resources.probability_so
import unitto.core.common.generated.resources.probability_sum
import unitto.core.common.generated.resources.probability_symbols
import unitto.core.common.generated.resources.probability_throws_column
import unitto.core.common.generated.resources.probability_variance_label
import unitto.core.common.generated.resources.probability_variance_name
import unitto.core.common.generated.resources.probability_water_add
import unitto.core.common.generated.resources.probability_water_enough
import unitto.core.common.generated.resources.probability_water_one
import unitto.core.common.generated.resources.probability_water_range
import unitto.core.common.generated.resources.probability_water_title
import unitto.core.common.generated.resources.probability_whole_population

/**
 * Card for the expected value and variance of a binomial, closed by default with both numbers in its
 * header. Open, it shows where they come from: one trial in a table, n times that, and for a few
 * trials the whole distribution. The ? opens a sheet with a worked example and the names and symbols.
 */
@Composable
internal fun BinomialMoments(n: Int, p: Double, fractional: String) {
  var shown by rememberSaveable { mutableStateOf(false) }
  var showHow by rememberSaveable { mutableStateOf(false) }
  ExplanationCard {
    MomentsHeader(n, p, fractional, shown, onClick = { shown = !shown }, onHelp = { showHow = true })
    if (shown) MomentsCard(n, p, fractional)
  }
  if (showHow) MomentsHowSheet(fractional) { showHow = false }
}

/** Title with `E(X) = 2.8 baskets on average` and `V(X) = 0.84` under it while closed, the ? and an arrow. */
@Composable
private fun MomentsHeader(n: Int, p: Double, fractional: String, shown: Boolean, onClick: () -> Unit, onHelp: () -> Unit) {
  val isCoin = abs(p - HALF) < SAME_TOLERANCE
  val average = stringResource(if (isCoin) Res.string.probability_heads_average else Res.string.probability_baskets_average)
  MomentsHeader((n * p).shown(fractional), " $average", (n * p * (1 - p)).shown(fractional), shown, onClick, onHelp)
}

@Composable
private fun MomentsHeader(
  mean: String,
  meanSupport: String,
  variance: String,
  shown: Boolean,
  onClick: () -> Unit,
  onHelp: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().clip(CELL_SHAPE).clickable(onClick = onClick),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        stringResource(Res.string.probability_moments),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      if (!shown) {
        Text(
          style = MaterialTheme.typography.bodyLarge,
          text =
            buildAnnotatedString {
              append("E(X) = ")
              withStyle(SpanStyle(color = MEAN_COLOR, fontWeight = FontWeight.Bold)) { append(mean) }
              append(meanSupport)
            },
        )
        Text(
          style = MaterialTheme.typography.bodyLarge,
          text =
            buildAnnotatedString {
              append("V(X) = ")
              withStyle(SpanStyle(color = VARIANCE_COLOR, fontWeight = FontWeight.Bold)) { append(variance) }
            },
        )
      }
    }
    if (shown) IconButton(onClick = onHelp) { Icon(Symbols.Help, stringResource(Res.string.probability_moments_how)) }
    Icon(if (shown) Symbols.KeyboardArrowUp else Symbols.KeyboardArrowDown, null)
  }
}

/**
 * Card for the normal law: E(X) = μ and V(X) = σ² in two tiles, then the water example of what happens
 * when the values of n people add up.
 */
@Composable
internal fun NormalMoments(mean: Double, sigma: Double, fractional: String) {
  var shown by rememberSaveable { mutableStateOf(false) }
  var showHow by rememberSaveable { mutableStateOf(false) }
  val variance = sigma * sigma
  ExplanationCard {
    MomentsHeader(mean.shown(fractional), "", variance.shown(fractional), shown, { shown = !shown }, { showHow = true })
    if (shown) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MomentTile(
          Modifier.weight(1f),
          Res.string.probability_mean_label,
          mean.shown(fractional),
          MEAN_COLOR,
          "μ, ${stringResource(Res.string.probability_curve_center)}",
        )
        MomentTile(
          Modifier.weight(1f),
          Res.string.probability_variance_label,
          variance.shown(fractional),
          VARIANCE_COLOR,
          "σ² = ${sigma.shown(fractional)}²",
        )
      }
      WaterExample(fractional)
    }
  }
  if (showHow) MomentsHowSheet(fractional) { showHow = false }
}

/**
 * One person drinks 2 L ± 0.7 L of water a day, the same example for any μ and σ. For n people the
 * mean and the variance are n times bigger, so σ only grows by √n. A curve shades the total within
 * 2σ of its mean, with the exact probabilities.
 */
@Composable
private fun WaterExample(fractional: String) {
  val mean = WATER_MEAN
  val sigma = WATER_SIGMA
  var countIndex by rememberSaveable { mutableIntStateOf(DEFAULT_COUNT_INDEX) }
  val count = SUM_COUNTS[countIndex]
  val totalMean = count * mean
  val totalVariance = count * sigma * sigma
  val totalSigma = sqrt(totalVariance)
  val lower = totalMean - 2 * totalSigma
  val upper = totalMean + 2 * totalSigma
  val total = Normal(totalMean, totalSigma)
  val unit = " L"
  fun Double.value() = shown(fractional, SUM_DECIMALS)
  fun Double.percent() = "${(this * PERCENT).shown(fractional, PERCENT_DECIMALS)} %"
  val countText = count.toString()

  Text(
    stringResource(Res.string.probability_water_title),
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.Bold,
  )
  Text(
    stringResource(Res.string.probability_water_one, mean.value(), sigma.value()),
    style = MaterialTheme.typography.bodyMedium,
  )

  // − 50 people +
  Row(verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = { countIndex-- }, enabled = countIndex > 0) { Text("−", style = MaterialTheme.typography.titleLarge) }
    Text(
      stringResource(Res.string.probability_people, countText),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )
    IconButton(onClick = { countIndex++ }, enabled = countIndex < SUM_COUNTS.lastIndex) {
      Text("+", style = MaterialTheme.typography.titleLarge)
    }
  }

  ColoredTable(
    header =
      listOf(
        Cell(""),
        Cell(stringResource(Res.string.probability_one_person)),
        Cell(""),
        Cell(stringResource(Res.string.probability_people, countText)),
      ),
    rows =
      listOf(
        listOf(Cell("E(X)"), Cell("${mean.value()}$unit", MEAN_COLOR), Cell("× $countText"), Cell("${totalMean.value()}$unit", MEAN_COLOR, bold = true)),
        listOf(
          Cell("V(X)"),
          Cell((sigma * sigma).value(), VARIANCE_COLOR),
          Cell("× $countText"),
          Cell(totalVariance.value(), VARIANCE_COLOR, bold = true),
        ),
        listOf(Cell("σ"), Cell("${sigma.value()}$unit"), Cell("√V"), Cell("${totalSigma.value()}$unit", bold = true)),
      ),
  )
  Text(
    stringResource(
      Res.string.probability_water_add,
      totalVariance.value(),
      totalSigma.value(),
      countText,
      sigma.value(),
      (count * sigma).value(),
    ),
    style = MaterialTheme.typography.bodyMedium,
  )

  DistributionGraph(
    modifier = Modifier.fillMaxWidth().height(SUM_GRAPH_HEIGHT),
    distribution = total,
    shaded = lower..upper,
    showLabels = true,
    showEdgeLabels = false,
  )
  Text(
    stringResource(
      Res.string.probability_water_range,
      lower.value(),
      upper.value(),
      (total.cdf(upper) - total.cdf(lower)).percent(),
    ),
    style = MaterialTheme.typography.bodyMedium,
  )
  Text(
    style = MaterialTheme.typography.bodyMedium,
    text =
      buildAnnotatedString {
        append(stringResource(Res.string.probability_so))
        append(" ")
        withStyle(SpanStyle(color = MEAN_COLOR, fontWeight = FontWeight.Bold)) {
          append("${totalMean.value()} + 2 × ${totalSigma.value()} ≈ ${upper.value()}$unit")
        }
        append(" ")
        append(
          stringResource(
            Res.string.probability_water_enough,
            countText,
            total.cdf(upper).percent(),
          )
        )
      },
  )
}

@Composable
private fun MomentsCard(n: Int, p: Double, fractional: String) {
  val isCoin = abs(p - HALF) < SAME_TOLERANCE
  val trialVariance = p * (1 - p)
  val mean = n * p
  val variance = n * trialVariance

  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    MomentTile(
      Modifier.weight(1f),
      Res.string.probability_mean_label,
      mean.shown(fractional),
      MEAN_COLOR,
      stringResource(if (isCoin) Res.string.probability_heads_average else Res.string.probability_baskets_average),
    )
    MomentTile(
      Modifier.weight(1f),
      Res.string.probability_variance_label,
      variance.shown(fractional),
      VARIANCE_COLOR,
      "σ ≈ ${sqrt(variance).shown(fractional, DEVIATION_DECIMALS)}",
    )
  }

  // Where p and p × (1 − p) come from: X is 0 or 1 for one trial
  Caption(stringResource(if (isCoin) Res.string.probability_one_flip_table else Res.string.probability_one_throw_table))
  MomentsTable(listOf(0.0, 1.0), listOf(1 - p, p), fractional)

  Caption(
    stringResource(if (isCoin) Res.string.probability_quick_flips else Res.string.probability_quick_throws, n.toString())
  )
  QuickTable(n, p, trialVariance, isCoin, fractional)

  if (n <= MAX_TABLE_TRIALS) {
    val distribution = Binomial(n, p)
    Caption(stringResource(Res.string.probability_from_probabilities))
    MomentsTable((0..n).map { it.toDouble() }, (0..n).map { distribution.density(it.toDouble()) }, fractional)
  }
}

/** Muted label above the big number. */
@Composable
private fun MomentTile(modifier: Modifier, label: StringResource, value: String, color: Color, support: String) {
  Column(
    modifier =
      modifier.clip(CELL_SHAPE).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(12.dp)
  ) {
    Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
    Text(support, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun Caption(text: String) {
  Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** One trial, × n, n trials for E(X) and V(X). */
@Composable
private fun QuickTable(n: Int, p: Double, trialVariance: Double, isCoin: Boolean, fractional: String) {
  val one = stringResource(if (isCoin) Res.string.probability_one_flip_column else Res.string.probability_one_throw_column)
  val all = stringResource(if (isCoin) Res.string.probability_flips_column else Res.string.probability_throws_column, n.toString())
  ColoredTable(
    header = listOf(Cell(""), Cell(one), Cell(""), Cell(all)),
    rows =
      listOf(
        listOf(Cell("E(X)"), Cell(p.shown(fractional), MEAN_COLOR), Cell("× $n"), Cell((n * p).shown(fractional), MEAN_COLOR, bold = true)),
        listOf(
          Cell("V(X)"),
          Cell(trialVariance.shown(fractional), VARIANCE_COLOR),
          Cell("× $n"),
          Cell((n * trialVariance).shown(fractional), VARIANCE_COLOR, bold = true),
        ),
      ),
  )
}

/**
 * x, P, x × P and (x − E)² × P for each value, with their sums: 1, E(X) and V(X). The same table for
 * one trial, for the whole distribution and for the worked example.
 */
@Composable
private fun MomentsTable(values: List<Double>, probabilities: List<Double>, fractional: String) {
  val mean = values.indices.sumOf { values[it] * probabilities[it] }
  val variance = values.indices.sumOf { (values[it] - mean).let { d -> d * d } * probabilities[it] }
  fun Double.cell() = shown(fractional, TABLE_DECIMALS)
  ColoredTable(
    header =
      listOf(Cell("x", VALUE_COLOR), Cell("P", PROBABILITY_COLOR), Cell("x × P", MEAN_COLOR), Cell("(x − E)² × P", VARIANCE_COLOR)),
    rows =
      values.indices.map { i ->
        val x = values[i]
        val probability = probabilities[i]
        listOf(
          Cell(x.cell(), VALUE_COLOR),
          Cell(probability.cell(), PROBABILITY_COLOR),
          Cell((x * probability).cell()),
          Cell(((x - mean) * (x - mean) * probability).cell()),
        )
      } +
        listOf(
          listOf(
            Cell(stringResource(Res.string.probability_sum), bold = true),
            Cell(probabilities.sum().cell()),
            Cell(mean.shown(fractional), MEAN_COLOR, bold = true),
            Cell(variance.shown(fractional), VARIANCE_COLOR, bold = true),
          )
        ),
    weights = listOf(0.8f, 1f, 1f, 1.5f),
  )
}

private class Cell(val text: String, val color: Color? = null, val bold: Boolean = false)

/** Rows of centered cells, the header in bold over a darker background. */
@Composable
private fun ColoredTable(header: List<Cell>, rows: List<List<Cell>>, weights: List<Float> = header.map { 1f }) {
  Column(modifier = Modifier.fillMaxWidth().clip(CELL_SHAPE).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
    (listOf(header) + rows).forEachIndexed { index, row ->
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .then(if (index == 0) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh) else Modifier)
            .padding(vertical = 6.dp)
      ) {
        row.forEachIndexed { column, cell ->
          Text(
            cell.text,
            modifier = Modifier.weight(weights[column]),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = cell.color ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = if (index == 0 || cell.bold) FontWeight.Bold else FontWeight.Normal,
          )
        }
      }
    }
  }
}

/** X = number of sports practiced in the same table, the two rules, then the names and symbols. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MomentsHowSheet(fractional: String, onDismiss: () -> Unit) {
  var showSymbols by rememberSaveable { mutableStateOf(false) }
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
    Column(
      modifier = Modifier.verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(stringResource(Res.string.probability_moments_how), style = MaterialTheme.typography.titleLarge)
      Text(stringResource(Res.string.probability_example_sports), style = MaterialTheme.typography.titleMedium)
      MomentsTable(EXAMPLE_VALUES, EXAMPLE_PROBABILITIES, fractional)
      Text(stringResource(Res.string.probability_how_expected), style = MaterialTheme.typography.bodyMedium)
      Text(stringResource(Res.string.probability_how_variance), style = MaterialTheme.typography.bodyMedium)

      Row(
        modifier = Modifier.fillMaxWidth().clip(CELL_SHAPE).clickable { showSymbols = !showSymbols }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(stringResource(Res.string.probability_symbols), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        Icon(if (showSymbols) Symbols.KeyboardArrowUp else Symbols.KeyboardArrowDown, null)
      }
      if (showSymbols) MomentSymbols()
    }
  }
}

/** Mean E(X) or μ, variance V(X), σ² or s², standard deviation √V(X), σ or s. */
@Composable
private fun MomentSymbols() {
  val population = stringResource(Res.string.probability_whole_population)
  val sample = stringResource(Res.string.probability_sample_of)
  val rows =
    listOf(
      Triple(Res.string.probability_mean_name, "E(X), μ", MEAN_COLOR),
      Triple(Res.string.probability_variance_name, "V(X), σ² ($population), s² ($sample)", VARIANCE_COLOR),
      Triple(Res.string.probability_deviation_name, "√V(X), σ ($population), s ($sample)", MaterialTheme.colorScheme.onSurface),
    )
  Column(
    modifier =
      Modifier.fillMaxWidth().clip(CELL_SHAPE).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    rows.forEach { (name, symbols, color) ->
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(name), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(symbols, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Bold)
      }
    }
  }
}

private const val HALF = 0.5
private const val SAME_TOLERANCE = 1e-12
// The table lists every P(X = k), short enough only for a few trials
private const val MAX_TABLE_TRIALS = 6
private const val TABLE_DECIMALS = 4
private const val DEVIATION_DECIMALS = 3
private val CELL_SHAPE = RoundedCornerShape(12.dp)

// The same color for each quantity in every table, readable in light and dark
private val VALUE_COLOR = Color(0xFF1E88E5)
private val PROBABILITY_COLOR = Color(0xFFFB8C00)
private val MEAN_COLOR = Color(0xFF43A047)
private val VARIANCE_COLOR = Color(0xFF8E24AA)

// The example: one person drinks 2 L ± 0.7 L of water a day
private const val WATER_MEAN = 2.0
private const val WATER_SIGMA = 0.7
// How many values add up in the example, 50 at first
private val SUM_COUNTS = listOf(2, 5, 10, 20, 50, 100, 200, 500, 1000)
private const val DEFAULT_COUNT_INDEX = 4
private const val SUM_DECIMALS = 2
private const val PERCENT = 100
private const val PERCENT_DECIMALS = 1
private val SUM_GRAPH_HEIGHT = 140.dp

// X = number of sports practiced, with the probability of each value
private val EXAMPLE_VALUES = listOf(0.0, 1.0, 2.0, 3.0)
private val EXAMPLE_PROBABILITIES = listOf(0.2, 0.15, 0.4, 0.25)
