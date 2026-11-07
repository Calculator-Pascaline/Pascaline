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

package com.sadellie.unitto.feature.calculator.vectors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.feature.calculator.electrical.Answers
import com.sadellie.unitto.feature.calculator.electrical.Hint
import com.sadellie.unitto.feature.calculator.electrical.ModeChips
import com.sadellie.unitto.feature.calculator.electrical.NameChips
import com.sadellie.unitto.feature.calculator.integrals.MultiFunction
import com.sadellie.unitto.feature.calculator.physics.LiveCard
import com.sadellie.unitto.feature.calculator.physics.PhysicsScreen
import com.sadellie.unitto.feature.calculator.physics.number
import com.sadellie.unitto.feature.calculator.physics.rememberFractional
import com.sadellie.unitto.feature.calculator.probability.rounded
import com.sadellie.unitto.feature.calculator.projectile.Answer
import com.sadellie.unitto.feature.calculator.projectile.Card
import com.sadellie.unitto.feature.calculator.projectile.Condition
import com.sadellie.unitto.feature.calculator.projectile.ConditionRow
import com.sadellie.unitto.feature.calculator.projectile.Step
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.projectile_explain_title
import unitto.core.common.generated.resources.vectors_at
import unitto.core.common.generated.resources.vectors_explain_div
import unitto.core.common.generated.resources.vectors_explain_div_text
import unitto.core.common.generated.resources.vectors_explain_grad
import unitto.core.common.generated.resources.vectors_explain_grad_text
import unitto.core.common.generated.resources.vectors_explain_laplacian
import unitto.core.common.generated.resources.vectors_explain_laplacian_text
import unitto.core.common.generated.resources.vectors_explain_rot
import unitto.core.common.generated.resources.vectors_explain_rot_text
import unitto.core.common.generated.resources.vectors_flow
import unitto.core.common.generated.resources.vectors_formulas
import unitto.core.common.generated.resources.vectors_hill
import unitto.core.common.generated.resources.vectors_hint
import unitto.core.common.generated.resources.vectors_invalid
import unitto.core.common.generated.resources.vectors_range
import unitto.core.common.generated.resources.vectors_saddle
import unitto.core.common.generated.resources.vectors_scalar
import unitto.core.common.generated.resources.vectors_source
import unitto.core.common.generated.resources.vectors_swirl
import unitto.core.common.generated.resources.vectors_title
import unitto.core.common.generated.resources.vectors_vector
import unitto.core.common.generated.resources.vectors_waves

private enum class FieldKind(val title: StringResource) {
  VECTOR(Res.string.vectors_vector),
  SCALAR(Res.string.vectors_scalar),
}

/** What the colors under the arrows show: what matters for each kind of field. */
private enum class FieldColor(val label: String, val kind: FieldKind) {
  DIVERGENCE("div F", FieldKind.VECTOR),
  ROTATION("rot F", FieldKind.VECTOR),
  LENGTH("|F|", FieldKind.VECTOR),
  VALUE("f", FieldKind.SCALAR),
  LAPLACIAN("Δf", FieldKind.SCALAR),
  SLOPE("|∇f|", FieldKind.SCALAR),
}

private class FieldExample(val name: StringResource, val kind: FieldKind, val first: String, val second: String = "")

private val EXAMPLES =
  listOf(
    FieldExample(Res.string.vectors_swirl, FieldKind.VECTOR, "−y", "x"),
    FieldExample(Res.string.vectors_source, FieldKind.VECTOR, "x", "y"),
    FieldExample(Res.string.vectors_flow, FieldKind.VECTOR, "1", "sin(x)"),
    FieldExample(Res.string.vectors_hill, FieldKind.SCALAR, "e^(−x² − y²)"),
    FieldExample(Res.string.vectors_saddle, FieldKind.SCALAR, "x² − y²"),
    FieldExample(Res.string.vectors_waves, FieldKind.SCALAR, "sin(x)·cos(y)"),
  )

private val RANGE = Condition("R", "", Res.string.vectors_range, 0.5..10.0, 0.5)

private fun read(text: String) = runCatching { MultiFunction(text) }.getOrNull()

/** A vector field with its divergence and rotation, or a scalar field with its gradient and Laplacian. */
@Composable
internal fun VectorFieldsRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  val format = { value: Double -> if (value.isFinite()) value.rounded(DECIMALS).replace(".", fractional) else "—" }
  var kind by rememberSaveable { mutableStateOf(FieldKind.VECTOR) }
  var p by rememberSaveable { mutableStateOf(EXAMPLES[0].first) }
  var q by rememberSaveable { mutableStateOf(EXAMPLES[0].second) }
  var f by rememberSaveable { mutableStateOf(EXAMPLES[3].first) }
  var vectorColor by rememberSaveable { mutableStateOf(FieldColor.DIVERGENCE) }
  var scalarColor by rememberSaveable { mutableStateOf(FieldColor.VALUE) }
  var range by rememberSaveable { mutableStateOf("3") }
  var probeX by rememberSaveable { mutableDoubleStateOf(1.0) }
  var probeY by rememberSaveable { mutableDoubleStateOf(1.0) }
  val reach = range.number().takeIf { it > 0 } ?: 1.0
  val color = if (kind == FieldKind.VECTOR) vectorColor else scalarColor
  // Only when a formula or the view changes: moving the point keeps the same picture
  val picture = remember(kind, color, p, q, f, reach) { picture(kind, color, read(p), read(q), read(f), reach) }

  PhysicsScreen(
    title = Res.string.vectors_title,
    openDrawer = openDrawer,
    live = {
      ModeChips(FieldKind.entries, kind, { it.title }) { kind = it }
      NameChips(FieldColor.entries.filter { it.kind == kind }, color, { it.label }) {
        if (kind == FieldKind.VECTOR) vectorColor = it else scalarColor = it
      }
      LiveCard {
        if (picture == null) {
          Text(stringResource(Res.string.vectors_invalid), color = MaterialTheme.colorScheme.error)
        } else {
          Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FieldView(picture, probeX to probeY) { x, y ->
              probeX = x
              probeY = y
            }
          }
          Hint(stringResource(Res.string.vectors_hint))
          PointValues(kind, read(p), read(q), read(f), probeX, probeY, format)
        }
      }
    },
    settings = {
      Card(stringResource(Res.string.vectors_formulas)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          EXAMPLES.filter { it.kind == kind }.forEach { example ->
            AssistChip(
              onClick = {
                if (kind == FieldKind.VECTOR) {
                  p = example.first
                  q = example.second
                } else {
                  f = example.first
                }
              },
              label = { Text(stringResource(example.name)) },
            )
          }
        }
        if (kind == FieldKind.VECTOR) {
          FormulaField("P(x, y)", p) { p = it }
          FormulaField("Q(x, y)", q) { q = it }
        } else {
          FormulaField("f(x, y)", f) { f = it }
        }
        ConditionRow(RANGE, range, fractional) { range = it }
      }
    },
    explanation = { FieldExplanation(kind, read(p), read(q), read(f), probeX, probeY, format) },
  )
}

/** Arrows of F or ∇f, on the colors of what was chosen. */
private fun picture(kind: FieldKind, color: FieldColor, p: MultiFunction?, q: MultiFunction?, f: MultiFunction?, reach: Double): FieldPicture? =
  if (kind == FieldKind.VECTOR) {
    if (p == null || q == null) {
      null
    } else {
      FieldPicture(reach, { x, y -> p.valueAt(x, y) to q.valueAt(x, y) }) { x, y ->
        val values = vectorAt(p, q, x, y)
        when (color) {
          FieldColor.ROTATION -> values.rotation
          FieldColor.LENGTH -> values.length
          else -> values.divergence
        }
      }
    }
  } else {
    f?.let {
      FieldPicture(reach, { x, y -> scalarAt(f, x, y).let { values -> values.gradientX to values.gradientY } }) { x, y ->
        when (color) {
          FieldColor.LAPLACIAN -> scalarAt(f, x, y).laplacian
          FieldColor.SLOPE -> scalarAt(f, x, y).slope
          else -> f.valueAt(x, y)
        }
      }
    }
  }

/** Everything at the point, with symbols that are the same in every language. */
@Composable
private fun PointValues(
  kind: FieldKind,
  p: MultiFunction?,
  q: MultiFunction?,
  f: MultiFunction?,
  x: Double,
  y: Double,
  format: (Double) -> String,
) {
  Text(stringResource(Res.string.vectors_at, "(${format(x)}, ${format(y)})"), style = MaterialTheme.typography.titleSmall)
  val tiles =
    if (kind == FieldKind.VECTOR && p != null && q != null) {
      val values = vectorAt(p, q, x, y)
      listOf(
        "F" to "(${format(values.p)}, ${format(values.q)})",
        "|F|" to format(values.length),
        "div F" to format(values.divergence),
        "rot F" to format(values.rotation),
      )
    } else if (f != null) {
      val values = scalarAt(f, x, y)
      listOf(
        "f" to format(values.value),
        "∇f" to "(${format(values.gradientX)}, ${format(values.gradientY)})",
        "|∇f|" to format(values.slope),
        "Δf" to format(values.laplacian),
      )
    } else {
      emptyList()
    }
  tiles.chunked(2).forEach { row ->
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      row.forEach { (label, value) -> Answer(Modifier.weight(1f), label, value) }
    }
  }
}

@Composable
private fun FormulaField(label: String, value: String, onChange: (String) -> Unit) {
  OutlinedTextField(
    value = value,
    onValueChange = onChange,
    modifier = Modifier.fillMaxWidth(),
    label = { Text(label) },
    singleLine = true,
    isError = read(value) == null,
    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii),
  )
}

@Composable
private fun FieldExplanation(
  kind: FieldKind,
  p: MultiFunction?,
  q: MultiFunction?,
  f: MultiFunction?,
  x: Double,
  y: Double,
  format: (Double) -> String,
) {
  Card(stringResource(Res.string.projectile_explain_title)) {
    if (kind == FieldKind.VECTOR && p != null && q != null) {
      val values = vectorAt(p, q, x, y)
      Step(Res.string.vectors_explain_div, Res.string.vectors_explain_div_text, "div F = ∂P/∂x + ∂Q/∂y = ${format(values.divergence)}")
      Step(Res.string.vectors_explain_rot, Res.string.vectors_explain_rot_text, "rot F = ∂Q/∂x − ∂P/∂y = ${format(values.rotation)}")
    } else if (f != null) {
      val values = scalarAt(f, x, y)
      Step(
        Res.string.vectors_explain_grad,
        Res.string.vectors_explain_grad_text,
        "∇f = (∂f/∂x, ∂f/∂y) = (${format(values.gradientX)}, ${format(values.gradientY)})",
      )
      Step(
        Res.string.vectors_explain_laplacian,
        Res.string.vectors_explain_laplacian_text,
        "Δf = ∂²f/∂x² + ∂²f/∂y² = ${format(values.laplacian)}",
      )
    }
  }
}

private const val DECIMALS = 3
