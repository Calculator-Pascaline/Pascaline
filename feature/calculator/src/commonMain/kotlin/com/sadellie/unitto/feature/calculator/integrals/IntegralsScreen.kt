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

package com.sadellie.unitto.feature.calculator.integrals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.feature.calculator.electrical.Answers
import com.sadellie.unitto.feature.calculator.electrical.Hint
import com.sadellie.unitto.feature.calculator.electrical.ModeChips
import com.sadellie.unitto.feature.calculator.physics.LiveCard
import com.sadellie.unitto.feature.calculator.physics.PhysicsScreen
import com.sadellie.unitto.feature.calculator.physics.rememberFractional
import com.sadellie.unitto.feature.calculator.probability.rounded
import com.sadellie.unitto.feature.calculator.projectile.Card
import com.sadellie.unitto.feature.calculator.projectile.Step
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.integrals_area
import unitto.core.common.generated.resources.integrals_average
import unitto.core.common.generated.resources.integrals_ball
import unitto.core.common.generated.resources.integrals_cone
import unitto.core.common.generated.resources.integrals_double
import unitto.core.common.generated.resources.integrals_explain_meaning
import unitto.core.common.generated.resources.integrals_explain_meaning_double_text
import unitto.core.common.generated.resources.integrals_explain_meaning_triple_text
import unitto.core.common.generated.resources.integrals_explain_order
import unitto.core.common.generated.resources.integrals_explain_order_text
import unitto.core.common.generated.resources.integrals_function
import unitto.core.common.generated.resources.integrals_hill
import unitto.core.common.generated.resources.integrals_hint
import unitto.core.common.generated.resources.integrals_invalid
import unitto.core.common.generated.resources.integrals_ramp
import unitto.core.common.generated.resources.integrals_title
import unitto.core.common.generated.resources.integrals_triple
import unitto.core.common.generated.resources.integrals_value
import unitto.core.common.generated.resources.integrals_volume
import unitto.core.common.generated.resources.projectile_explain_title

private enum class IntegralKind(val title: StringResource) {
  DOUBLE(Res.string.integrals_double),
  TRIPLE(Res.string.integrals_triple),
}

/** What is typed: the function and the bounds, from the outside in. */
private data class IntegralTexts(
  val function: String,
  val xLow: String,
  val xHigh: String,
  val yLow: String,
  val yHigh: String,
  val zLow: String = "",
  val zHigh: String = "",
) {
  /** null when something can't be read. */
  fun setup(kind: IntegralKind): IntegralSetup? =
    runCatching {
        val isTriple = kind == IntegralKind.TRIPLE
        IntegralSetup(
          function = MultiFunction(function),
          xLow = MultiFunction(xLow).valueAt(0.0),
          xHigh = MultiFunction(xHigh).valueAt(0.0),
          y = Bounds(MultiFunction(yLow), MultiFunction(yHigh)),
          z = if (isTriple) Bounds(MultiFunction(zLow), MultiFunction(zHigh)) else null,
        )
      }
      .getOrNull()
      ?.takeIf { it.xLow.isFinite() && it.xHigh.isFinite() && it.xLow != it.xHigh }
}

private class Example(val name: StringResource, val kind: IntegralKind, val texts: IntegralTexts)

private const val DISK_LOW = "−√(1 − x²)"
private const val DISK_HIGH = "√(1 − x²)"
private val EXAMPLES =
  listOf(
    Example(Res.string.integrals_ramp, IntegralKind.DOUBLE, IntegralTexts("x·y + 2", "0", "2", "0", "x²")),
    Example(Res.string.integrals_hill, IntegralKind.DOUBLE, IntegralTexts("4 − x² − y²", "−1", "1", DISK_LOW, DISK_HIGH)),
    Example(
      Res.string.integrals_ball,
      IntegralKind.TRIPLE,
      IntegralTexts("1", "−1", "1", DISK_LOW, DISK_HIGH, "−√(1 − x² − y²)", "√(1 − x² − y²)"),
    ),
    Example(Res.string.integrals_cone, IntegralKind.TRIPLE, IntegralTexts("1", "−1", "1", DISK_LOW, DISK_HIGH, "√(x² + y²)", "1")),
  )

private fun format(value: Double, fractional: String) = if (value.isFinite()) value.rounded(DECIMALS).replace(".", fractional) else "—"

/** Double and triple integrals: the shape they measure in 3D, and their value. */
@Composable
internal fun IntegralsRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  var kind by rememberSaveable { mutableStateOf(IntegralKind.DOUBLE) }
  var double by rememberSaveable(stateSaver = TEXTS_SAVER) { mutableStateOf(EXAMPLES[0].texts) }
  var triple by rememberSaveable(stateSaver = TEXTS_SAVER) { mutableStateOf(EXAMPLES[2].texts) }
  val texts = if (kind == IntegralKind.DOUBLE) double else triple
  fun edit(change: IntegralTexts) {
    if (kind == IntegralKind.DOUBLE) double = change else triple = change
  }
  val setup = texts.setup(kind)
  // Thousands of values of f: away from the screen
  val result by
    produceState<IntegralResult?>(null, setup?.let { texts to kind }) {
      value = setup?.let { withContext(Dispatchers.Default) { runCatching { it.integrate() }.getOrNull() } }
    }
  val faces by
    produceState(emptyList<Face>(), setup?.let { texts to kind }) {
      value = setup?.let { withContext(Dispatchers.Default) { runCatching { it.faces(VIEW_STEPS) }.getOrNull() } }.orEmpty()
    }

  PhysicsScreen(
    title = Res.string.integrals_title,
    openDrawer = openDrawer,
    live = {
      ModeChips(IntegralKind.entries, kind, { it.title }) { kind = it }
      IntegralCard(kind, faces, result.takeIf { setup != null }, fractional)
    },
    settings = { IntegralInputs(kind, texts, ::edit) },
    explanation = { result?.takeIf { it.isValid && setup != null }?.let { IntegralExplanation(kind, texts, it, fractional) } },
  )
}

/** The drawing, then the value with what it means. */
@Composable
private fun IntegralCard(kind: IntegralKind, faces: List<Face>, result: IntegralResult?, fractional: String) {
  LiveCard {
    IntegralView(faces)
    Hint(stringResource(Res.string.integrals_hint))
    if (result == null || !result.isValid) {
      Text(stringResource(Res.string.integrals_invalid), color = MaterialTheme.colorScheme.error)
      return@LiveCard
    }
    Text(
      text = "${if (kind == IntegralKind.DOUBLE) "∬" else "∭"} f = ${format(result.value, fractional)}",
      modifier = Modifier.fillMaxWidth(),
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      textAlign = TextAlign.Center,
    )
    Answers(
      Res.string.integrals_value to format(result.value, fractional),
      (if (kind == IntegralKind.DOUBLE) Res.string.integrals_area else Res.string.integrals_volume) to format(result.size, fractional),
      Res.string.integrals_average to format(result.average, fractional),
    )
  }
}

/** Examples to start from, the function, then the bounds from the outside in. */
@Composable
private fun IntegralInputs(kind: IntegralKind, texts: IntegralTexts, edit: (IntegralTexts) -> Unit) {
  Card(stringResource(Res.string.integrals_function)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      EXAMPLES.filter { it.kind == kind }
        .forEach { example -> AssistChip(onClick = { edit(example.texts) }, label = { Text(stringResource(example.name)) }) }
    }
    ExpressionField(if (kind == IntegralKind.DOUBLE) "f(x, y)" else "f(x, y, z)", texts.function) { edit(texts.copy(function = it)) }
    BoundRow("x", texts.xLow, texts.xHigh, { edit(texts.copy(xLow = it)) }) { edit(texts.copy(xHigh = it)) }
    BoundRow("y", texts.yLow, texts.yHigh, { edit(texts.copy(yLow = it)) }) { edit(texts.copy(yHigh = it)) }
    if (kind == IntegralKind.TRIPLE) {
      BoundRow("z", texts.zLow, texts.zHigh, { edit(texts.copy(zLow = it)) }) { edit(texts.copy(zHigh = it)) }
    }
  }
}

@Composable
private fun ExpressionField(label: String, value: String, onChange: (String) -> Unit) {
  OutlinedTextField(
    value = value,
    onValueChange = onChange,
    modifier = Modifier.fillMaxWidth(),
    label = { Text(label) },
    singleLine = true,
    isError = runCatching { MultiFunction(value) }.isFailure,
    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii),
  )
}

/** low ≤ x ≤ high: the bounds of the inner directions can use the outer letters. */
@Composable
private fun BoundRow(name: String, low: String, high: String, onLow: (String) -> Unit, onHigh: (String) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    BoundField(low, onLow, Modifier.weight(1f))
    Text("≤ $name ≤", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    BoundField(high, onHigh, Modifier.weight(1f))
  }
}

@Composable
private fun BoundField(value: String, onChange: (String) -> Unit, modifier: Modifier) {
  OutlinedTextField(
    value = value,
    onValueChange = onChange,
    modifier = modifier,
    singleLine = true,
    isError = runCatching { MultiFunction(value) }.isFailure,
    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii),
  )
}

@Composable
private fun IntegralExplanation(kind: IntegralKind, texts: IntegralTexts, result: IntegralResult, fractional: String) {
  val inner = if (kind == IntegralKind.TRIPLE) "∫[${texts.zLow}, ${texts.zHigh}] " else ""
  val ends = if (kind == IntegralKind.TRIPLE) "dz dy dx" else "dy dx"
  Card(stringResource(Res.string.projectile_explain_title)) {
    Step(
      Res.string.integrals_explain_order,
      Res.string.integrals_explain_order_text,
      "∫[${texts.xLow}, ${texts.xHigh}] ∫[${texts.yLow}, ${texts.yHigh}] $inner(${texts.function}) $ends",
      "= ${format(result.value, fractional)}",
    )
    Step(
      Res.string.integrals_explain_meaning,
      if (kind == IntegralKind.DOUBLE) Res.string.integrals_explain_meaning_double_text else Res.string.integrals_explain_meaning_triple_text,
      "${format(result.value, fractional)} / ${format(result.size, fractional)} = ${format(result.average, fractional)}",
    )
  }
}

private val TEXTS_SAVER =
  androidx.compose.runtime.saveable.Saver<IntegralTexts, String>(
    save = { listOf(it.function, it.xLow, it.xHigh, it.yLow, it.yHigh, it.zLow, it.zHigh).joinToString(SEPARATOR) },
    restore = { saved ->
      val parts = saved.split(SEPARATOR)
      IntegralTexts(parts[0], parts[1], parts[2], parts[3], parts[4], parts.getOrElse(Z_LOW) { "" }, parts.getOrElse(Z_HIGH) { "" })
    },
  )

private const val SEPARATOR = "\u0001"
private const val Z_LOW = 5
private const val Z_HIGH = 6
private const val DECIMALS = 4
private const val VIEW_STEPS = 18
