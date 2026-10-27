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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.probability_coefficients_explanation
import unitto.core.common.generated.resources.probability_coefficients_question
import unitto.core.common.generated.resources.probability_coin
import unitto.core.common.generated.resources.probability_coin_preset
import unitto.core.common.generated.resources.probability_examples
import unitto.core.common.generated.resources.probability_expected_baskets
import unitto.core.common.generated.resources.probability_expected_heads
import unitto.core.common.generated.resources.probability_expected_value
import unitto.core.common.generated.resources.probability_free_throws
import unitto.core.common.generated.resources.probability_free_throws_preset
import unitto.core.common.generated.resources.probability_head
import unitto.core.common.generated.resources.probability_heads
import unitto.core.common.generated.resources.probability_mean_check
import unitto.core.common.generated.resources.probability_mean_explanation
import unitto.core.common.generated.resources.probability_miss_letter
import unitto.core.common.generated.resources.probability_pascal_row
import unitto.core.common.generated.resources.probability_score_and_miss
import unitto.core.common.generated.resources.probability_score_letter
import unitto.core.common.generated.resources.probability_tap_to_use
import unitto.core.common.generated.resources.probability_tree_legend
import unitto.core.common.generated.resources.probability_variance
import unitto.core.common.generated.resources.probability_variance_baskets
import unitto.core.common.generated.resources.probability_variance_explanation
import unitto.core.common.generated.resources.probability_variance_heads

/** Example of the parameters step: tapping it fills n and p. */
private class Preset(val description: @Composable () -> String, val n: String, val p: String)

/** Two everyday binomial situations under the parameters. */
@Composable
internal fun BinomialPresets(fractional: String, onPick: (n: String, p: String) -> Unit) {
  val presets =
    listOf(
      Preset({ stringResource(Res.string.probability_coin_preset) }, "3", "0.5"),
      Preset({ stringResource(Res.string.probability_free_throws_preset, "4", "0.7".decimal(fractional)) }, "4", "0.7"),
    )
  Column(verticalArrangement = Arrangement.spacedBy(CARD_SPACING)) {
    SectionTitle(stringResource(Res.string.probability_examples))
    presets.forEach { preset ->
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .clip(EXAMPLE_SHAPE)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onPick(preset.n, preset.p) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(preset.description(), style = MaterialTheme.typography.bodyLarge)
        Text(
          text = "n = ${preset.n}   p = ${preset.p.decimal(fractional)}",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium,
        )
        Text(
          text = stringResource(Res.string.probability_tap_to_use),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

/**
 * Where the binomial formula comes from, for the n and p that were entered. With p = ½ it is n flips
 * of a fair coin (with a probability tree for a few flips), otherwise n free throws.
 */
@Composable
internal fun BinomialExplanation(n: Int, p: Double, fractional: String) {
  Column(verticalArrangement = Arrangement.spacedBy(CARD_SPACING * 2)) {
    if (n <= MAX_LINES) {
      ExplanationCard {
        if (abs(p - HALF) < SAME_TOLERANCE) CoinExplanation(n) else FreeThrowExplanation(n, p, fractional)
      }
    }
    if (n <= MAX_TRIANGLE_ROW) {
      // Row n has the arrow colors when there is a tree
      ExplanationCard { PascalTriangle(n, coloredRow = n in 1..MAX_TREE_FLIPS) }
    }
  }
}

/** What the moment buttons show under the equation. */
private enum class ShownMoment {
  EXPECTED_VALUE,
  VARIANCE,
}

/**
 * Buttons for E(X) = n × p and V(X) = n × p × (1 − p), each opening a card that explains where the
 * formula comes from, with the entered n and p.
 */
@Composable
internal fun BinomialMoments(n: Int, p: Double, fractional: String) {
  MomentButtons(expectedValue = { ExpectedValue(n, p, fractional) }, variance = { Variance(n, p, fractional) })
}

/** Expected value and variance buttons, the selected one opens its card under them. */
@Composable
internal fun MomentButtons(expectedValue: @Composable () -> Unit, variance: @Composable () -> Unit) {
  var shown by rememberSaveable { mutableStateOf<ShownMoment?>(null) }
  Column(verticalArrangement = Arrangement.spacedBy(CARD_SPACING * 2)) {
    Row(horizontalArrangement = Arrangement.spacedBy(CARD_SPACING)) {
      FilterChip(
        selected = shown == ShownMoment.EXPECTED_VALUE,
        onClick = { shown = if (shown == ShownMoment.EXPECTED_VALUE) null else ShownMoment.EXPECTED_VALUE },
        label = { Text(stringResource(Res.string.probability_expected_value), style = MaterialTheme.typography.titleSmall) },
      )
      FilterChip(
        selected = shown == ShownMoment.VARIANCE,
        onClick = { shown = if (shown == ShownMoment.VARIANCE) null else ShownMoment.VARIANCE },
        label = { Text(stringResource(Res.string.probability_variance), style = MaterialTheme.typography.titleSmall) },
      )
    }
    when (shown) {
      ShownMoment.EXPECTED_VALUE -> ExplanationCard { expectedValue() }
      ShownMoment.VARIANCE -> ExplanationCard { variance() }
      null -> {}
    }
  }
}

/** E(X) = n × p = 5 × 0.5 = 2.5, why, and for a few trials the same number from the probabilities. */
@Composable
private fun ExpectedValue(n: Int, p: Double, fractional: String) {
  val isCoin = abs(p - HALF) < SAME_TOLERANCE
  val mean = n * p
  Text(
    stringResource(
      if (isCoin) Res.string.probability_expected_heads else Res.string.probability_expected_baskets,
      n.toString(),
    ),
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
  )
  MomentFormula("E(X) = n × p = $n × ${p.shown(fractional)} = ", mean.shown(fractional))
  Text(stringResource(Res.string.probability_mean_explanation), style = MaterialTheme.typography.bodyMedium)
  if (n <= MAX_CHECK_TRIALS) {
    // 0 × P(X = 0) + 1 × P(X = 1) + … gives the same number
    val distribution = Binomial(n, p)
    val terms =
      // Non-breaking spaces keep each k × P(X = k) on one line
      (0..n).joinToString(" + ") { k ->
        "$k × ${distribution.density(k.toDouble()).shown(fractional, CHECK_DECIMALS)}"
      }
    Text(
      stringResource(Res.string.probability_mean_check),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text("E(X) = $terms = ${mean.shown(fractional)}", style = MaterialTheme.typography.bodyMedium)
  }
}

/** V(X) = n × p × (1 − p), why, and the standard deviation σ = √V. */
@Composable
private fun Variance(n: Int, p: Double, fractional: String) {
  val isCoin = abs(p - HALF) < SAME_TOLERANCE
  val variance = n * p * (1 - p)
  Text(
    stringResource(
      if (isCoin) Res.string.probability_variance_heads else Res.string.probability_variance_baskets,
      n.toString(),
      (n * p).shown(fractional),
    ),
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
  )
  MomentFormula(
    "V(X) = n × p × (1 − p) = $n × ${p.shown(fractional)} × ${(1 - p).shown(fractional)} = ",
    variance.shown(fractional),
  )
  MomentFormula("σ(X) = √V(X) ≈ ", sqrt(variance).shown(fractional, CHECK_DECIMALS))
  Text(stringResource(Res.string.probability_variance_explanation), style = MaterialTheme.typography.bodyMedium)
}

/** Formula with its result in the primary color: `E(X) = n × p = 5 × 0.5 = ` **2.5**. */
@Composable
internal fun MomentFormula(formula: String, result: String) {
  Text(
    style = MaterialTheme.typography.titleMedium,
    text =
      buildAnnotatedString {
        append(formula)
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) { append(result) }
      },
  )
}

/** 0.7 ==> 0,7 with a comma separator, without rounding errors. */
internal fun Double.shown(fractional: String, decimals: Int = VALUE_DECIMALS) = rounded(decimals).decimal(fractional)

@Composable
internal fun ExplanationCard(content: @Composable () -> Unit) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(EXAMPLE_SHAPE)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    content()
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(
    modifier = Modifier.padding(horizontal = 4.dp),
    text = text,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

/**
 * n flips of a fair coin: k heads → C(n,k) × (½)ⁿ. Up to [MAX_TREE_FLIPS] flips, a probability tree
 * shows every outcome, with arrows in the color of each label pointing to the leaves it counts.
 */
@Composable
private fun CoinExplanation(flips: Int) {
  Text(
    stringResource(Res.string.probability_coin, flips.toString()),
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
  )
  val labels =
    (0..flips).map { heads ->
      val name =
        if (heads == 1) stringResource(Res.string.probability_head)
        else stringResource(Res.string.probability_heads, heads.toString())
      buildAnnotatedString {
        append("$name → ")
        withStyle(SpanStyle(color = arrowColor(heads), fontWeight = FontWeight.Bold)) {
          append(binomialCoefficient(flips, heads).toString())
        }
        append(" × (½)${flips.superscript()}")
      }
    }
  if (flips in 1..MAX_TREE_FLIPS) {
    val measurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface
    val branchStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val leafStyle = MaterialTheme.typography.labelLarge.copy(color = textColor, fontWeight = FontWeight.Medium)
    val labelStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor)
    Canvas(modifier = Modifier.fillMaxWidth().height(LEAF_HEIGHT * (1 shl flips))) {
      drawBinomialTree(
        TreeStyle(flips, "T", "H", 'T', 'H', COIN_TREE_SHARE),
        measurer,
        labels,
        lineColor,
        branchStyle,
        leafStyle,
        labelStyle,
      )
    }
  } else {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      labels.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
    }
  }
}

/**
 * Branch texts and leaf letters of a tree: failure goes up, success goes down.
 *
 * @property treeShare Part of the width for branches, the rest is for leaves and labels.
 */
private class TreeStyle(
  val levels: Int,
  val failureBranch: String,
  val successBranch: String,
  val failureLetter: Char,
  val successLetter: Char,
  val treeShare: Float,
)

/** Probability tree with [labels] on the right and arrows from label k to every leaf with k successes. */
private fun DrawScope.drawBinomialTree(
  tree: TreeStyle,
  measurer: TextMeasurer,
  labels: List<AnnotatedString>,
  lineColor: Color,
  branchStyle: TextStyle,
  leafStyle: TextStyle,
  labelStyle: TextStyle,
) {
  val flips = tree.levels
  val leaves = 1 shl flips
  val leafHeight = size.height / leaves
  val treeWidth = size.width * tree.treeShare
  val levelWidth = treeWidth / flips
  val start = 4.dp.toPx()
  fun leafY(index: Int) = (index + 0.5f) * leafHeight
  // Node at [level] covering leaves from [first]: centered on them
  fun nodeY(level: Int, first: Int) = (leafY(first) + leafY(first + (1 shl (flips - level)) - 1)) / 2

  // Branches: failure up, success down
  for (level in 0 until flips) {
    val span = 1 shl (flips - level)
    for (first in 0 until leaves step span) {
      val from = Offset(start + level * levelWidth, nodeY(level, first))
      listOf(first to false, first + span / 2 to true).forEach { (childFirst, isSuccess) ->
        val to = Offset(start + (level + 1) * levelWidth, nodeY(level + 1, childFirst))
        drawLine(lineColor, from, to, strokeWidth = 2f)
        val label = measurer.measure(if (isSuccess) tree.successBranch else tree.failureBranch, branchStyle)
        val middle = (from + to) / 2f
        val above = if (!isSuccess) -label.size.height.toFloat() else 0f
        drawText(label, topLeft = Offset(middle.x - label.size.width / 2, middle.y + above))
      }
      drawCircle(lineColor, radius = 3.dp.toPx(), center = from)
    }
  }

  // Leaves: outcome like THH, then arrows from the labels
  val labelLayouts = labels.map { measurer.measure(it, labelStyle) }
  val labelX = size.width - labelLayouts.maxOf { it.size.width } - 2.dp.toPx()
  val leafEnds = mutableListOf<Pair<Offset, Int>>()
  for (index in 0 until leaves) {
    val outcome =
      (flips - 1 downTo 0).joinToString("") { bit ->
        (if (index shr bit and 1 == 1) tree.successLetter else tree.failureLetter).toString()
      }
    val layout = measurer.measure(outcome, leafStyle)
    val y = leafY(index)
    drawText(layout, topLeft = Offset(start + treeWidth + 6.dp.toPx(), y - layout.size.height / 2))
    leafEnds += Offset(start + treeWidth + 10.dp.toPx() + layout.size.width, y) to outcome.count { it == tree.successLetter }
  }
  labelLayouts.forEachIndexed { heads, layout ->
    val ends = leafEnds.filter { it.second == heads }.map { it.first }
    val y = ends.map { it.y }.average().toFloat()
    drawText(layout, topLeft = Offset(labelX, y - layout.size.height / 2))
    ends.forEach { end -> drawArrow(Offset(labelX - 4.dp.toPx(), y), end, arrowColor(heads)) }
  }
}

/** Line from [from] to [to] with a head at [to]. */
private fun DrawScope.drawArrow(from: Offset, to: Offset, color: Color) {
  val width = 2.dp.toPx()
  drawLine(color, from, to, strokeWidth = width)
  val angle = atan2(from.y - to.y, from.x - to.x)
  val head = ARROW_HEAD.toPx()
  listOf(angle + ARROW_ANGLE, angle - ARROW_ANGLE).forEach { side ->
    drawLine(color, to, Offset(to.x + head * cos(side), to.y + head * sin(side)), strokeWidth = width)
  }
}

/** n free throws, p to score: k → C(n,k) × (1−p)ⁿ⁻ᵏ × pᵏ for every number of baskets k. */
@Composable
private fun FreeThrowExplanation(throws: Int, p: Double, fractional: String) {
  Text(
    stringResource(Res.string.probability_free_throws, throws.toString()),
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
  )
  val score = p.rounded(VALUE_DECIMALS).decimal(fractional)
  val miss = (1 - p).rounded(VALUE_DECIMALS).decimal(fractional)
  Text(
    stringResource(Res.string.probability_score_and_miss, score, miss),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  val hasTree = throws in 1..MAX_TREE_FLIPS
  val labels =
    (0..throws).map { made ->
      buildAnnotatedString {
        append("$made → ")
        val color = if (hasTree) arrowColor(made) else MaterialTheme.colorScheme.primary
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
          append(binomialCoefficient(throws, made).toString())
        }
        val misses = throws - made
        if (misses > 0) append(" × ($miss)${misses.superscript()}")
        if (made > 0) append(" × ($score)${made.superscript()}")
      }
    }
  if (hasTree) {
    // S = score, M = miss, branches show their probability
    Text(
      stringResource(Res.string.probability_tree_legend),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val measurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface
    val branchStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val leafStyle = MaterialTheme.typography.labelLarge.copy(color = textColor, fontWeight = FontWeight.Medium)
    val labelStyle = MaterialTheme.typography.bodySmall.copy(color = textColor)
    val missLetter = stringResource(Res.string.probability_miss_letter).first()
    val scoreLetter = stringResource(Res.string.probability_score_letter).first()
    Canvas(modifier = Modifier.fillMaxWidth().height(LEAF_HEIGHT * (1 shl throws))) {
      drawBinomialTree(
        TreeStyle(throws, miss, score, missLetter, scoreLetter, THROW_TREE_SHARE),
        measurer,
        labels,
        lineColor,
        branchStyle,
        leafStyle,
        labelStyle,
      )
    }
  } else {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      labels.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
    }
  }
}

/**
 * Rows 0 to [n] of Pascal's triangle, row n underlined: its numbers are the coefficients above.
 * With the coin tree, row n has the colors of the arrows. Numbers get smaller to fit.
 */
@Composable
private fun PascalTriangle(n: Int, coloredRow: Boolean) {
  Text(
    stringResource(Res.string.probability_coefficients_question),
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
  )
  Text(
    stringResource(Res.string.probability_coefficients_explanation),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  val highlight = MaterialTheme.colorScheme.primary
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    // Room for the "n = …" label on the right
    val cell = min(PASCAL_CELL, (maxWidth - PASCAL_LABEL_WIDTH) / (n + 1))
    val style =
      if (cell < PASCAL_CELL) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      (0..n).forEach { row ->
        val isUsed = row == n
        Box(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier =
              Modifier.align(Alignment.Center).drawBehind {
                if (isUsed) {
                  val y = size.height + 1.dp.toPx()
                  drawLine(highlight, Offset(0f, y), Offset(size.width, y), strokeWidth = 3.dp.toPx())
                }
              }
          ) {
            (0..row).forEach { k ->
              val color =
                when {
                  !isUsed -> MaterialTheme.colorScheme.onSurface
                  coloredRow -> arrowColor(k)
                  else -> highlight
                }
              Text(
                modifier = Modifier.width(cell),
                text = binomialCoefficient(row, k).toString(),
                textAlign = TextAlign.Center,
                style = style,
                color = color,
                fontWeight = if (isUsed) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
              )
            }
          }
          if (isUsed) {
            Text(
              modifier = Modifier.align(Alignment.CenterEnd),
              text = stringResource(Res.string.probability_pascal_row, row.toString()),
              style = MaterialTheme.typography.labelMedium.copy(color = highlight),
            )
          }
        }
      }
    }
  }
}

/** n choose k. */
internal fun binomialCoefficient(n: Int, k: Int): Long {
  if (k < 0 || k > n) return 0
  var result = 1L
  for (i in 1..minOf(k, n - k)) result = result * (n - minOf(k, n - k) + i) / i
  return result
}

/** Color of the label for k heads, the same in the tree and in the triangle. */
private fun arrowColor(k: Int): Color = ARROW_COLORS[k % ARROW_COLORS.size]

private fun Int.superscript(): String = toString().map { SUPERSCRIPTS[it - '0'] }.joinToString("")

/** 0.7 with the decimal separator of the settings. */
private fun String.decimal(fractional: String) = replace(".", fractional)

private const val HALF = 0.5
private const val SAME_TOLERANCE = 1e-12
private const val MAX_TREE_FLIPS = 4
private const val MAX_LINES = 20
private const val MAX_TRIANGLE_ROW = 12
private const val VALUE_DECIMALS = 6
// The check lists every P(X = k), short enough only for a few trials
private const val MAX_CHECK_TRIALS = 6
private const val CHECK_DECIMALS = 4
private const val SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹"

// One color per number of heads, readable in light and dark
private val ARROW_COLORS =
  listOf(Color(0xFF1E88E5), Color(0xFFE53935), Color(0xFF43A047), Color(0xFF8E24AA), Color(0xFFFB8C00))
private const val COIN_TREE_SHARE = 0.36f
// Labels with two powers are longer
private const val THROW_TREE_SHARE = 0.3f
private val LEAF_HEIGHT = 32.dp
private val ARROW_HEAD = 7.dp
private const val ARROW_ANGLE = 0.45f
private val PASCAL_CELL = 34.dp
private val PASCAL_LABEL_WIDTH = 44.dp
private val CARD_SPACING = 8.dp
private val EXAMPLE_SHAPE = RoundedCornerShape(20.dp)
