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

package io.github.calculatorpascaline.feature.calculator.geometry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.calculatorpascaline.core.ui.LeftToRight
import io.github.calculatorpascaline.feature.calculator.electrical.Hint
import io.github.calculatorpascaline.feature.calculator.electrical.ModeChips
import io.github.calculatorpascaline.feature.calculator.electrical.NameChips
import io.github.calculatorpascaline.feature.calculator.physics.InvalidValues
import io.github.calculatorpascaline.feature.calculator.physics.LiveCard
import io.github.calculatorpascaline.feature.calculator.physics.PhysicsScreen
import io.github.calculatorpascaline.feature.calculator.physics.numberFormat
import io.github.calculatorpascaline.feature.calculator.physics.rememberFractional
import io.github.calculatorpascaline.feature.calculator.projectile.Answer
import io.github.calculatorpascaline.feature.calculator.probability.toNumberOrNull
import io.github.calculatorpascaline.feature.calculator.projectile.Card
import kotlin.math.PI
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.geometry_acute
import pascaline.core.common.generated.resources.geometry_angle
import pascaline.core.common.generated.resources.geometry_angles_hint
import pascaline.core.common.generated.resources.geometry_arc
import pascaline.core.common.generated.resources.geometry_area
import pascaline.core.common.generated.resources.geometry_chord
import pascaline.core.common.generated.resources.geometry_circumference
import pascaline.core.common.generated.resources.geometry_circumradius
import pascaline.core.common.generated.resources.geometry_clear
import pascaline.core.common.generated.resources.geometry_diagonals
import pascaline.core.common.generated.resources.geometry_diameter
import pascaline.core.common.generated.resources.geometry_equilateral
import pascaline.core.common.generated.resources.geometry_formulas
import pascaline.core.common.generated.resources.geometry_given
import pascaline.core.common.generated.resources.geometry_heights
import pascaline.core.common.generated.resources.geometry_inradius
import pascaline.core.common.generated.resources.geometry_isosceles
import pascaline.core.common.generated.resources.geometry_lateral
import pascaline.core.common.generated.resources.geometry_no_triangle
import pascaline.core.common.generated.resources.geometry_obtuse
import pascaline.core.common.generated.resources.geometry_optional
import pascaline.core.common.generated.resources.geometry_perimeter
import pascaline.core.common.generated.resources.geometry_radius
import pascaline.core.common.generated.resources.geometry_right
import pascaline.core.common.generated.resources.geometry_scalene
import pascaline.core.common.generated.resources.geometry_sector
import pascaline.core.common.generated.resources.geometry_segment
import pascaline.core.common.generated.resources.geometry_side
import pascaline.core.common.generated.resources.geometry_slant
import pascaline.core.common.generated.resources.geometry_surface
import pascaline.core.common.generated.resources.geometry_title
import pascaline.core.common.generated.resources.geometry_triangle_hint
import pascaline.core.common.generated.resources.geometry_two_triangles
import pascaline.core.common.generated.resources.geometry_values
import pascaline.core.common.generated.resources.geometry_volume

/**
 * Geometry: a triangle from any three of its values, quadrilaterals, a circle and its sectors, and solids. Each is drawn
 * to scale, with what can be worked out from it and the formulas.
 */
@Composable
internal fun GeometryRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  val format = numberFormat(fractional)
  var inputs by rememberSaveable(stateSaver = INPUTS_SAVER) { mutableStateOf(GeometryInputs()) }
  PhysicsScreen(
    title = Res.string.geometry_title,
    openDrawer = openDrawer,
    live = {
      LiveCard {
        ModeChips(Shape.entries, inputs.shape, { it.title }) { inputs = inputs.copy(shape = it) }
        when (inputs.shape) {
          Shape.TRIANGLE -> TriangleView(inputs, format)
          Shape.QUADRILATERAL -> {
            ModeChips(Quad.entries, inputs.quad, { it.title }) { inputs = inputs.copy(quad = it) }
            QuadView(inputs, format)
          }
          Shape.CIRCLE -> CircleView(inputs, format)
          Shape.SOLID -> {
            ModeChips(SolidKind.entries, inputs.solid, { it.title }) { inputs = inputs.copy(solid = it) }
            SolidView(inputs, format)
          }
        }
      }
    },
    settings = { ValuesCard(inputs, format) { inputs = it } },
    explanation = { FormulasCard(inputs) },
  )
}

/** What is shown in a result tile: "Area S", "6". */
private class Shown(val label: StringResource, val symbol: String, val value: String)

/** The fields of the shape picked. */
private fun GeometryInputs.fields(): List<Field> =
  when (shape) {
    Shape.TRIANGLE -> TRIANGLE_FIELDS
    Shape.QUADRILATERAL -> quad.fields
    Shape.CIRCLE -> listOf(given.field(), CIRCLE_ANGLE)
    Shape.SOLID -> solid.fields
  }

private fun shown(format: (Double) -> String): (Double) -> String = { if (it.isFinite()) format(it) else "—" }

@Composable
private fun Results(results: List<Shown>) {
  results.chunked(2).forEach { row ->
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      row.forEach { Answer(Modifier.weight(1f), "${stringResource(it.label)} ${it.symbol}".trim(), it.value) }
      if (row.size == 1) Spacer(Modifier.weight(1f))
    }
  }
}

@Composable
private fun Drawing(figure: Figure) = FigureView(figure, Modifier.fillMaxWidth().height(FIGURE_HEIGHT))

@Composable
private fun TriangleView(inputs: GeometryInputs, format: (Double) -> String) {
  val known = TRIANGLE_FIELDS.map { field -> inputs.value(field).takeIf { inputs.text(field).isNotBlank() } }
  val found = solveTriangle(known.take(3), known.drop(3))
  var pick by remember(found.size) { mutableIntStateOf(0) }
  val triangle = found.getOrNull(pick)
  if (triangle == null) {
    val enough = known.count { it != null } >= 3 && known.take(3).any { it != null }
    Hint(stringResource(if (enough) Res.string.geometry_no_triangle else Res.string.geometry_triangle_hint))
  } else {
    if (found.size > 1) {
      Hint(stringResource(Res.string.geometry_two_triangles))
      NameChips(found.indices.toList(), pick, { "${it + 1}" }) { pick = it }
    }
    Drawing(triangleFigure(triangle))
    val angleKind =
      when (triangle.angleKind) {
        AngleKind.ACUTE -> Res.string.geometry_acute
        AngleKind.RIGHT -> Res.string.geometry_right
        AngleKind.OBTUSE -> Res.string.geometry_obtuse
      }
    val sideKind =
      when (triangle.sideKind) {
        SideKind.SCALENE -> Res.string.geometry_scalene
        SideKind.ISOSCELES -> Res.string.geometry_isosceles
        SideKind.EQUILATERAL -> Res.string.geometry_equilateral
      }
    Text("${stringResource(angleKind)} · ${stringResource(sideKind)}", style = MaterialTheme.typography.titleMedium)
    Results(triangleResults(triangle, shown(format)))
  }
}

private fun triangleResults(triangle: Triangle, show: (Double) -> String): List<Shown> {
  val sides = triangle.sides.mapIndexed { i, side -> Shown(Res.string.geometry_side, "abc"[i].toString(), show(side)) }
  val angles =
    triangle.angles.mapIndexed { i, angle -> Shown(Res.string.geometry_angle, "ABC"[i].toString(), "${show(angle * HALF_TURN_DEGREES / PI)}°") }
  return sides.zip(angles).flatMap { (side, angle) -> listOf(side, angle) } +
    listOf(
      Shown(Res.string.geometry_area, "S", show(triangle.area)),
      Shown(Res.string.geometry_perimeter, "P", show(triangle.perimeter)),
      Shown(Res.string.geometry_inradius, "r", show(triangle.inradius)),
      Shown(Res.string.geometry_circumradius, "R", show(triangle.circumradius)),
      Shown(Res.string.geometry_heights, "", triangle.heights.joinToString(" · ") { show(it) }),
    )
}

private fun Quad.flat(x: List<Double>): Flat =
  when (this) {
    Quad.SQUARE -> square(x[0])
    Quad.RECTANGLE -> rectangle(x[0], x[1])
    Quad.PARALLELOGRAM -> parallelogram(x[0], x[1], x[2])
    Quad.RHOMBUS -> rhombus(x[0], x[1])
    Quad.TRAPEZOID -> x.takeLast(2).let { (left, right) -> trapezoid(x[0], x[1], x[2], left, right) }
    Quad.KITE -> x.takeLast(2).let { (sideA, sideB) -> kite(x[0], x[1], sideA, sideB) }
  }

@Composable
private fun QuadView(inputs: GeometryInputs, format: (Double) -> String) {
  val fields = inputs.quad.fields
  if (!inputs.valid(fields)) return InvalidValues()
  val x = fields.map(inputs::value)
  val flat = inputs.quad.flat(x)
  val show = shown(format)
  Drawing(quadFigure(inputs.quad, x))
  Results(
    listOfNotNull(
      Shown(Res.string.geometry_area, "S", show(flat.area)),
      Shown(Res.string.geometry_perimeter, "P", show(flat.perimeter)),
      flat.diagonals.takeIf { it.isNotEmpty() }?.let { d -> Shown(Res.string.geometry_diagonals, "d", d.joinToString(" · ") { show(it) }) },
    )
  )
}

@Composable
private fun CircleView(inputs: GeometryInputs, format: (Double) -> String) {
  val fields = listOf(inputs.given.field(), CIRCLE_ANGLE)
  if (!inputs.valid(fields)) return InvalidValues()
  val circle = Circle(radiusFrom(inputs.given, inputs.value(fields[0])), inputs.value(CIRCLE_ANGLE))
  val show = shown(format)
  Drawing(circleFigure(circle.radius, circle.angle))
  val sector =
    if (circle.angle.isFinite()) {
      listOf(
        Shown(Res.string.geometry_arc, "l", show(circle.arc)),
        Shown(Res.string.geometry_sector, "", show(circle.sectorArea)),
        Shown(Res.string.geometry_chord, "", show(circle.chord)),
        Shown(Res.string.geometry_segment, "", show(circle.segmentArea)),
      )
    } else {
      emptyList()
    }
  Results(
    listOf(
      Shown(Res.string.geometry_radius, "r", show(circle.radius)),
      Shown(Res.string.geometry_diameter, "d", show(circle.diameter)),
      Shown(Res.string.geometry_circumference, "C", show(circle.circumference)),
      Shown(Res.string.geometry_area, "S", show(circle.area)),
    ) + sector
  )
}

private fun SolidKind.solid(x: List<Double>): Solid =
  when (this) {
    SolidKind.CUBE -> cube(x[0])
    SolidKind.CUBOID -> cuboid(x[0], x[1], x[2])
    SolidKind.SPHERE -> sphere(x[0])
    SolidKind.CYLINDER -> cylinder(x[0], x[1])
    SolidKind.CONE -> cone(x[0], x[1])
    SolidKind.PYRAMID -> squarePyramid(x[0], x[1])
  }

@Composable
private fun SolidView(inputs: GeometryInputs, format: (Double) -> String) {
  val fields = inputs.solid.fields
  if (!inputs.valid(fields)) return InvalidValues()
  val x = fields.map(inputs::value)
  val solid = inputs.solid.solid(x)
  val show = shown(format)
  Drawing(solidFigure(inputs.solid, x))
  Results(
    listOfNotNull(
      Shown(Res.string.geometry_volume, "V", show(solid.volume)),
      Shown(Res.string.geometry_surface, "S", show(solid.surface)),
      Shown(Res.string.geometry_lateral, "", show(solid.lateral)).takeIf { solid.lateral.isFinite() },
      Shown(Res.string.geometry_slant, "s", show(solid.slant)).takeIf { solid.slant.isFinite() },
    )
  )
}

@Composable
private fun ValuesCard(inputs: GeometryInputs, format: (Double) -> String, change: (GeometryInputs) -> Unit) {
  val fields = inputs.fields()
  Card(stringResource(Res.string.geometry_values)) {
    if (inputs.shape == Shape.CIRCLE) {
      Hint(stringResource(Res.string.geometry_given))
      ModeChips(CircleGiven.entries, inputs.given, { it.title() }) { given -> change(inputs.givenBy(given, format)) }
    }
    fields.forEach { field ->
      FieldRow(field, inputs.text(field), showOptional = inputs.shape != Shape.TRIANGLE) { change(inputs.with(field, it)) }
    }
    if (inputs.shape == Shape.TRIANGLE) {
      Hint(stringResource(Res.string.geometry_triangle_hint))
      TextButton(onClick = { change(inputs.copy(values = inputs.values + TRIANGLE_FIELDS.associate { it.key to "" })) }) {
        Text(stringResource(Res.string.geometry_clear))
      }
    }
    if (fields.any { it.angle }) Hint(stringResource(Res.string.geometry_angles_hint))
  }
}

/** The same circle, given by something else. */
private fun GeometryInputs.givenBy(given: CircleGiven, format: (Double) -> String): GeometryInputs {
  val field = this.given.field()
  val radius = radiusFrom(this.given, value(field))
  val text = if (radius.isFinite()) format(valueFrom(given, radius)) else text(field)
  return copy(given = given).with(field, text)
}

@Composable
private fun FieldRow(field: Field, value: String, showOptional: Boolean, onValueChange: (String) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      field.symbol,
      modifier = Modifier.width(SYMBOL_WIDTH),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(stringResource(field.label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(
      modifier = Modifier.width(FIELD_WIDTH),
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      isError = value.isNotBlank() && value.toNumberOrNull() == null,
      placeholder = if (field.optional && showOptional) ({ Text(stringResource(Res.string.geometry_optional)) }) else null,
      suffix = if (field.angle) ({ Text("°") }) else null,
      textStyle = MaterialTheme.typography.bodyMedium,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
  }
}

@Composable
private fun FormulasCard(inputs: GeometryInputs) {
  val formulas =
    when (inputs.shape) {
      Shape.TRIANGLE -> TRIANGLE_FORMULAS
      Shape.QUADRILATERAL -> inputs.quad.formulas
      Shape.CIRCLE -> CIRCLE_FORMULAS
      Shape.SOLID -> inputs.solid.formulas
    }
  Card(stringResource(Res.string.geometry_formulas)) {
    LeftToRight { formulas.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) } }
  }
}

private val FIGURE_HEIGHT = 220.dp
private val SYMBOL_WIDTH = 36.dp
private val FIELD_WIDTH = 140.dp
