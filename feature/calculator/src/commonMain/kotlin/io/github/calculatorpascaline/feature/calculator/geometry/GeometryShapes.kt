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

import androidx.compose.runtime.saveable.listSaver
import io.github.calculatorpascaline.feature.calculator.probability.toNumberOrNull
import kotlin.math.PI
import org.jetbrains.compose.resources.StringResource
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.geometry_angle
import pascaline.core.common.generated.resources.geometry_area
import pascaline.core.common.generated.resources.geometry_base
import pascaline.core.common.generated.resources.geometry_bottom
import pascaline.core.common.generated.resources.geometry_central_angle
import pascaline.core.common.generated.resources.geometry_circle
import pascaline.core.common.generated.resources.geometry_circumference
import pascaline.core.common.generated.resources.geometry_cone
import pascaline.core.common.generated.resources.geometry_cube
import pascaline.core.common.generated.resources.geometry_cuboid
import pascaline.core.common.generated.resources.geometry_cylinder
import pascaline.core.common.generated.resources.geometry_diagonal
import pascaline.core.common.generated.resources.geometry_diameter
import pascaline.core.common.generated.resources.geometry_height
import pascaline.core.common.generated.resources.geometry_kite
import pascaline.core.common.generated.resources.geometry_leg
import pascaline.core.common.generated.resources.geometry_length
import pascaline.core.common.generated.resources.geometry_parallelogram
import pascaline.core.common.generated.resources.geometry_pyramid
import pascaline.core.common.generated.resources.geometry_quadrilateral
import pascaline.core.common.generated.resources.geometry_radius
import pascaline.core.common.generated.resources.geometry_rectangle
import pascaline.core.common.generated.resources.geometry_rhombus
import pascaline.core.common.generated.resources.geometry_side
import pascaline.core.common.generated.resources.geometry_solids
import pascaline.core.common.generated.resources.geometry_sphere
import pascaline.core.common.generated.resources.geometry_square
import pascaline.core.common.generated.resources.geometry_top
import pascaline.core.common.generated.resources.geometry_trapezoid
import pascaline.core.common.generated.resources.geometry_triangle
import pascaline.core.common.generated.resources.geometry_width

/** A value to type: its [symbol] on the drawing, what it is, and whether it is an [angle] in degrees. */
internal class Field(
  val key: String,
  val symbol: String,
  val label: StringResource,
  val default: String = "",
  val angle: Boolean = false,
  val optional: Boolean = false,
)

internal enum class Shape(val title: StringResource) {
  TRIANGLE(Res.string.geometry_triangle),
  QUADRILATERAL(Res.string.geometry_quadrilateral),
  CIRCLE(Res.string.geometry_circle),
  SOLID(Res.string.geometry_solids),
}

internal enum class Quad(val title: StringResource, val fields: List<Field>, val formulas: List<String>) {
  SQUARE(
    Res.string.geometry_square,
    listOf(Field("sq.a", "a", Res.string.geometry_side, "4")),
    listOf("S = a²", "P = 4a", "d = a·√2"),
  ),
  RECTANGLE(
    Res.string.geometry_rectangle,
    listOf(Field("re.a", "a", Res.string.geometry_width, "6"), Field("re.b", "b", Res.string.geometry_height, "4")),
    listOf("S = a·b", "P = 2(a + b)", "d = √(a² + b²)"),
  ),
  PARALLELOGRAM(
    Res.string.geometry_parallelogram,
    listOf(
      Field("pa.a", "a", Res.string.geometry_base, "6"),
      Field("pa.b", "b", Res.string.geometry_side, "4"),
      Field("pa.alpha", "α", Res.string.geometry_angle, "60", angle = true),
    ),
    listOf("S = a·b·sin α", "P = 2(a + b)", "d² = a² + b² ± 2ab·cos α"),
  ),
  RHOMBUS(
    Res.string.geometry_rhombus,
    listOf(Field("rh.a", "a", Res.string.geometry_side, "5"), Field("rh.alpha", "α", Res.string.geometry_angle, "60", angle = true)),
    listOf("S = a²·sin α", "P = 4a", "S = d₁·d₂ / 2"),
  ),
  TRAPEZOID(
    Res.string.geometry_trapezoid,
    listOf(
      Field("tr.a", "a", Res.string.geometry_bottom, "8"),
      Field("tr.b", "b", Res.string.geometry_top, "4"),
      Field("tr.h", "h", Res.string.geometry_height, "3"),
      Field("tr.c", "c", Res.string.geometry_leg, optional = true),
      Field("tr.d", "d", Res.string.geometry_leg, optional = true),
    ),
    listOf("S = (a + b) / 2 · h", "P = a + b + c + d"),
  ),
  KITE(
    Res.string.geometry_kite,
    listOf(
      Field("ki.p", "d₁", Res.string.geometry_diagonal, "8"),
      Field("ki.q", "d₂", Res.string.geometry_diagonal, "6"),
      Field("ki.a", "a", Res.string.geometry_side, optional = true),
      Field("ki.b", "b", Res.string.geometry_side, optional = true),
    ),
    listOf("S = d₁·d₂ / 2", "P = 2(a + b)"),
  ),
}

internal enum class SolidKind(val title: StringResource, val fields: List<Field>, val formulas: List<String>) {
  CUBE(Res.string.geometry_cube, listOf(Field("cu.a", "a", Res.string.geometry_side, "3")), listOf("V = a³", "S = 6a²")),
  CUBOID(
    Res.string.geometry_cuboid,
    listOf(
      Field("bo.l", "l", Res.string.geometry_length, "5"),
      Field("bo.w", "w", Res.string.geometry_width, "3"),
      Field("bo.h", "h", Res.string.geometry_height, "2"),
    ),
    listOf("V = l·w·h", "S = 2(lw + lh + wh)"),
  ),
  SPHERE(Res.string.geometry_sphere, listOf(Field("sp.r", "r", Res.string.geometry_radius, "3")), listOf("V = 4/3·π·r³", "S = 4πr²")),
  CYLINDER(
    Res.string.geometry_cylinder,
    listOf(Field("cy.r", "r", Res.string.geometry_radius, "2"), Field("cy.h", "h", Res.string.geometry_height, "5")),
    listOf("V = πr²h", "S = 2πr(r + h)"),
  ),
  CONE(
    Res.string.geometry_cone,
    listOf(Field("co.r", "r", Res.string.geometry_radius, "3"), Field("co.h", "h", Res.string.geometry_height, "4")),
    listOf("V = πr²h / 3", "s = √(r² + h²)", "S = πr(r + s)"),
  ),
  PYRAMID(
    Res.string.geometry_pyramid,
    listOf(Field("py.a", "a", Res.string.geometry_side, "6"), Field("py.h", "h", Res.string.geometry_height, "4")),
    listOf("V = a²·h / 3", "s = √((a/2)² + h²)", "S = a² + 2as"),
  ),
}

internal val TRIANGLE_FIELDS =
  listOf(
    Field("t.a", "a", Res.string.geometry_side, "7", optional = true),
    Field("t.b", "b", Res.string.geometry_side, "5", optional = true),
    Field("t.c", "c", Res.string.geometry_side, optional = true),
    Field("t.A", "A", Res.string.geometry_angle, angle = true, optional = true),
    Field("t.B", "B", Res.string.geometry_angle, angle = true, optional = true),
    Field("t.C", "C", Res.string.geometry_angle, "60", angle = true, optional = true),
  )

internal val TRIANGLE_FORMULAS =
  listOf(
    "A + B + C = 180°",
    "a / sin A = b / sin B = c / sin C",
    "c² = a² + b² − 2ab·cos C",
    "S = ½·a·b·sin C",
    "S = √(p(p − a)(p − b)(p − c)), p = P / 2",
    "r = S / p, R = a / (2·sin A)",
  )

internal val CIRCLE_FORMULAS =
  listOf("d = 2r", "C = 2πr", "S = πr²", "l = r·θ", "S(sector) = r²·θ / 2", "chord = 2r·sin(θ / 2)", "S(segment) = r² / 2 · (θ − sin θ)")

internal fun CircleGiven.title(): StringResource =
  when (this) {
    CircleGiven.RADIUS -> Res.string.geometry_radius
    CircleGiven.DIAMETER -> Res.string.geometry_diameter
    CircleGiven.CIRCUMFERENCE -> Res.string.geometry_circumference
    CircleGiven.AREA -> Res.string.geometry_area
  }

internal fun CircleGiven.field() =
  Field(
    "c.value",
    when (this) {
      CircleGiven.RADIUS -> "r"
      CircleGiven.DIAMETER -> "d"
      CircleGiven.CIRCUMFERENCE -> "C"
      CircleGiven.AREA -> "S"
    },
    title(),
    "3",
  )

internal val CIRCLE_ANGLE = Field("c.theta", "θ", Res.string.geometry_central_angle, "60", angle = true, optional = true)

/** The [given] value of a circle of [radius]. */
internal fun valueFrom(given: CircleGiven, radius: Double): Double =
  when (given) {
    CircleGiven.RADIUS -> radius
    CircleGiven.DIAMETER -> 2 * radius
    CircleGiven.CIRCUMFERENCE -> 2 * PI * radius
    CircleGiven.AREA -> PI * radius * radius
  }

/** Everything picked and typed. Fields not typed yet show their default. */
internal data class GeometryInputs(
  val shape: Shape = Shape.TRIANGLE,
  val quad: Quad = Quad.RECTANGLE,
  val solid: SolidKind = SolidKind.CYLINDER,
  val given: CircleGiven = CircleGiven.RADIUS,
  val values: Map<String, String> = emptyMap(),
) {
  fun text(field: Field): String = values[field.key] ?: field.default

  /** The number in [field], in radians for an angle; NaN when empty or not a number. */
  fun value(field: Field): Double {
    val number = text(field).toNumberOrNull() ?: Double.NaN
    return if (field.angle) number * PI / HALF_TURN_DEGREES else number
  }

  /** Whether every needed field holds a number above 0, and every angle is under 180°. */
  fun valid(fields: List<Field>): Boolean =
    fields.all { field ->
      val value = value(field)
      (field.optional && text(field).isBlank()) || (value > 0 && (!field.angle || value < PI))
    }

  fun with(field: Field, text: String) = copy(values = values + (field.key to text))
}

internal val INPUTS_SAVER =
  listSaver<GeometryInputs, String>(
    save = { inputs ->
      listOf(inputs.shape.name, inputs.quad.name, inputs.solid.name, inputs.given.name) +
        inputs.values.flatMap { (key, text) -> listOf(key, text) }
    },
    restore = { saved ->
      GeometryInputs(
        Shape.valueOf(saved[0]),
        Quad.valueOf(saved[1]),
        SolidKind.valueOf(saved[2]),
        CircleGiven.valueOf(saved[3]),
        saved.drop(SAVED_NAMES).chunked(2).associate { (key, text) -> key to text },
      )
    },
  )

private const val SAVED_NAMES = 4
internal const val HALF_TURN_DEGREES = 180.0
