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

package io.github.calculatorpascaline.feature.calculator.surfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.calculatorpascaline.feature.calculator.electrical.Hint
import io.github.calculatorpascaline.feature.calculator.integrals.MultiFunction
import io.github.calculatorpascaline.feature.calculator.physics.LiveCard
import io.github.calculatorpascaline.feature.calculator.physics.PhysicsScreen
import io.github.calculatorpascaline.feature.calculator.physics.rememberFractional
import io.github.calculatorpascaline.feature.calculator.probability.rounded
import io.github.calculatorpascaline.feature.calculator.projectile.Card
import io.github.calculatorpascaline.feature.calculator.projectile.Step
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.surfaces_3d
import pascaline.core.common.generated.resources.surfaces_at
import pascaline.core.common.generated.resources.surfaces_divergence
import pascaline.core.common.generated.resources.surfaces_field_text
import pascaline.core.common.generated.resources.surfaces_field_title
import pascaline.core.common.generated.resources.surfaces_flat
import pascaline.core.common.generated.resources.surfaces_function
import pascaline.core.common.generated.resources.surfaces_gradient
import pascaline.core.common.generated.resources.surfaces_hint_3d
import pascaline.core.common.generated.resources.surfaces_hint_levels
import pascaline.core.common.generated.resources.surfaces_invalid
import pascaline.core.common.generated.resources.surfaces_levels
import pascaline.core.common.generated.resources.surfaces_maximum
import pascaline.core.common.generated.resources.surfaces_minimum
import pascaline.core.common.generated.resources.surfaces_none
import pascaline.core.common.generated.resources.surfaces_points
import pascaline.core.common.generated.resources.surfaces_rotation
import pascaline.core.common.generated.resources.surfaces_rotation_zero
import pascaline.core.common.generated.resources.surfaces_saddle
import pascaline.core.common.generated.resources.surfaces_slope
import pascaline.core.common.generated.resources.surfaces_tap_point
import pascaline.core.common.generated.resources.surfaces_test_text
import pascaline.core.common.generated.resources.surfaces_test_title
import pascaline.core.common.generated.resources.surfaces_title

/** What is typed: f and the region where it is drawn. */
private data class SurfaceTexts(val function: String, val xLow: String, val xHigh: String, val yLow: String, val yHigh: String) {
  fun parsed(): Pair<MultiFunction, Region>? =
    runCatching {
        val bound = { text: String -> MultiFunction(text).valueAt(0.0) }
        MultiFunction(function) to Region(bound(xLow), bound(xHigh), bound(yLow), bound(yHigh))
      }
      .getOrNull()
      ?.takeIf { it.second.isValid }
}

private val TEXTS_SAVER =
  androidx.compose.runtime.saveable.listSaver<SurfaceTexts, String>(
    save = { listOf(it.function, it.xLow, it.xHigh, it.yLow, it.yHigh) },
    restore = { SurfaceTexts(it[0], it[1], it[2], it[3], it[4]) },
  )

/** A minimum and a saddle, a saddle, a hill, many hills, and a saddle the test can't tell. */
private val EXAMPLES =
  listOf("x³ − 3x + y²", "x² − y²", "e^(−x² − y²)", "sin(x)·cos(y)", "x³ − 3x·y²").map { SurfaceTexts(it, "−2", "2", "−2", "2") }

private enum class SurfaceView {
  THREE_D,
  LEVELS,
}

/** Everything worked out from f, away from the screen. */
private class SurfaceResult(val surface: SurfaceGrid, val map: SurfaceGrid, val points: List<CriticalPoint>)

/** z = f(x, y) in 3D or as level curves, with its maxima, minima and saddle points and ∇f. */
@Composable
internal fun SurfacesRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  var texts by rememberSaveable(stateSaver = TEXTS_SAVER) { mutableStateOf(EXAMPLES[0]) }
  var view by rememberSaveable { mutableStateOf(SurfaceView.LEVELS) }
  var probe by rememberSaveable { mutableStateOf<Pair<Double, Double>?>(null) }
  var selected by rememberSaveable { mutableStateOf<Int?>(null) }
  val parsed = texts.parsed()
  val result by
    produceState<SurfaceResult?>(null, texts) {
      value =
        parsed?.let { (function, region) ->
          withContext(Dispatchers.Default) {
            runCatching {
                SurfaceResult(function.grid(region, SURFACE_STEPS), function.grid(region, MAP_STEPS), function.criticalPoints(region))
              }
              .getOrNull()
          }
        }
    }
  val format = { value: Double -> if (value.isFinite()) value.rounded(DECIMALS).replace(".", fractional) else "—" }
  PhysicsScreen(
    title = Res.string.surfaces_title,
    openDrawer = openDrawer,
    live = {
      TextInput("f(x, y)", texts.function, Modifier.fillMaxWidth()) {
        texts = texts.copy(function = it)
        probe = null
        selected = null
      }
      ViewTabs(view) { view = it }
      val shown = result
      LiveCard {
        if (parsed == null || shown == null) {
          Text(stringResource(Res.string.surfaces_invalid), color = MaterialTheme.colorScheme.error)
          return@LiveCard
        }
        if (view == SurfaceView.THREE_D) {
          SurfaceView(shown.surface, shown.points, selected)
          HeightBar(shown.surface, format)
          Hint(stringResource(Res.string.surfaces_hint_3d))
        } else {
          LevelCurvesView(shown.map, shown.points, probe) { x, y ->
            probe = x to y
            selected = null
          }
          Hint(stringResource(Res.string.surfaces_hint_levels))
        }
        probe?.let { (x, y) -> PointValues(parsed.first, x, y, format) }
      }
      shown?.let { PointList(it.points, selected, format) { index -> selected = index; probe = it.points[index].x to it.points[index].y } }
    },
    settings = { SurfaceInputs(texts) { texts = it; probe = null; selected = null } },
    explanation = { SurfaceExplanation() },
  )
}

/** The colors of the graph from its lowest z to its highest. */
@Composable
private fun HeightBar(grid: SurfaceGrid, format: (Double) -> String) {
  val scheme = MaterialTheme.colorScheme
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("z = ${format(grid.low)}", style = MaterialTheme.typography.labelMedium)
    Box(
      Modifier.weight(1f)
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(Brush.horizontalGradient(listOf(scheme.primaryContainer, scheme.primary)))
    )
    Text(format(grid.high), style = MaterialTheme.typography.labelMedium)
  }
}

@Composable
private fun ViewTabs(selected: SurfaceView, onSelect: (SurfaceView) -> Unit) {
  val labels = listOf(Res.string.surfaces_3d, Res.string.surfaces_levels)
  SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    SurfaceView.entries.forEachIndexed { index, view ->
      SegmentedButton(
        selected = view == selected,
        onClick = { onSelect(view) },
        shape = SegmentedButtonDefaults.itemShape(index, SurfaceView.entries.size),
        label = { Text(stringResource(labels[index])) },
      )
    }
  }
}

/** f, ∇f, its divergence and its rotation where the map was tapped. */
@Composable
private fun PointValues(function: MultiFunction, x: Double, y: Double, format: (Double) -> String) {
  val values = scalarAt(function, x, y)
  Text(stringResource(Res.string.surfaces_at, format(x), format(y)), style = MaterialTheme.typography.titleSmall)
  ValueLine(Res.string.surfaces_function, "f = ${format(values.value)}")
  ValueLine(Res.string.surfaces_gradient, "(${format(values.gradientX)}, ${format(values.gradientY)})")
  ValueLine(Res.string.surfaces_slope, format(values.slope))
  ValueLine(Res.string.surfaces_divergence, "Δf = ${format(values.laplacian)}")
  ValueLine(Res.string.surfaces_rotation, "0")
  Hint(stringResource(Res.string.surfaces_rotation_zero))
}

@Composable
private fun ValueLine(label: StringResource, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(stringResource(label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
  }
}

/** Each point where ∇f = 0, a tap shows its test. */
@Composable
private fun PointList(points: List<CriticalPoint>, selected: Int?, format: (Double) -> String, onSelect: (Int) -> Unit) {
  Card(stringResource(Res.string.surfaces_points)) {
    if (points.isEmpty()) Text(stringResource(Res.string.surfaces_none), style = MaterialTheme.typography.bodyMedium)
    else Hint(stringResource(Res.string.surfaces_tap_point))
    points.forEachIndexed { index, point ->
      val isSelected = index == selected
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
            .clickable { onSelect(index) }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Box(Modifier.size(12.dp).clip(CircleShape).background(point.kind.color))
          Text(stringResource(point.kind.title), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
          Text("(${format(point.x)}, ${format(point.y)})", style = MaterialTheme.typography.bodyMedium)
          Text("z = ${format(point.z)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        if (isSelected) {
          Text(
            "fxx = ${format(point.hessian.fxx)} · fyy = ${format(point.hessian.fyy)} · fxy = ${format(point.hessian.fxy)}",
            style = MaterialTheme.typography.bodySmall,
          )
          Text("D = fxx·fyy − fxy² = ${format(point.hessian.determinant)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

private val PointKind.title: StringResource
  get() =
    when (this) {
      PointKind.MAXIMUM -> Res.string.surfaces_maximum
      PointKind.MINIMUM -> Res.string.surfaces_minimum
      PointKind.SADDLE -> Res.string.surfaces_saddle
      PointKind.FLAT -> Res.string.surfaces_flat
    }

@Composable
private fun SurfaceInputs(texts: SurfaceTexts, onChange: (SurfaceTexts) -> Unit) {
  Card(stringResource(Res.string.surfaces_function)) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      EXAMPLES.forEach { example -> AssistChip(onClick = { onChange(example) }, label = { Text(example.function) }) }
    }
    BoundRow("x", texts.xLow, texts.xHigh, { onChange(texts.copy(xLow = it)) }) { onChange(texts.copy(xHigh = it)) }
    BoundRow("y", texts.yLow, texts.yHigh, { onChange(texts.copy(yLow = it)) }) { onChange(texts.copy(yHigh = it)) }
  }
}

@Composable
private fun BoundRow(name: String, low: String, high: String, onLow: (String) -> Unit, onHigh: (String) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    TextInput(null, low, Modifier.weight(1f), onLow)
    Text("≤ $name ≤", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    TextInput(null, high, Modifier.weight(1f), onHigh)
  }
}

@Composable
private fun TextInput(label: String?, value: String, modifier: Modifier, onChange: (String) -> Unit) {
  OutlinedTextField(
    value = value,
    onValueChange = onChange,
    modifier = modifier,
    label = label?.let { { Text(it) } },
    singleLine = true,
    isError = runCatching { MultiFunction(value) }.isFailure,
    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii),
  )
}

/** The second derivative test, then what ∇f, its divergence and its rotation mean here. */
@Composable
private fun SurfaceExplanation() {
  Card(stringResource(Res.string.surfaces_test_title)) {
    Step(Res.string.surfaces_test_title, Res.string.surfaces_test_text, "∇f = (fx, fy) = (0, 0)", "D = fxx·fyy − fxy²")
    Step(
      Res.string.surfaces_field_title,
      Res.string.surfaces_field_text,
      "div ∇f = fxx + fyy = Δf",
      "rot ∇f = ∂fy/∂x − ∂fx/∂y = fyx − fxy = 0",
    )
  }
}

private const val SURFACE_STEPS = 40
private const val MAP_STEPS = 48
private const val DECIMALS = 4
