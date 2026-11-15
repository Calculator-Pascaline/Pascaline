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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A triangle: [sides] a, b, c and [angles] A, B, C in radians, each angle opposite the side of the
 * same index.
 */
internal class Triangle(val sides: List<Double>, val angles: List<Double>) {
  val perimeter: Double
    get() = sides.sum()

  /** ½·b·c·sin A */
  val area: Double
    get() = sides[1] * sides[2] * sin(angles[0]) / 2

  /** The height onto each side: 2·area / side. */
  val heights: List<Double>
    get() = sides.map { 2 * area / it }

  /** Radius of the circle inside it that touches the 3 sides: area / half the perimeter. */
  val inradius: Double
    get() = area / (perimeter / 2)

  /** Radius of the circle through its 3 corners: a / (2·sin A). */
  val circumradius: Double
    get() = sides[0] / (2 * sin(angles[0]))

  val angleKind: AngleKind
    get() {
      val largest = angles.max()
      return when {
        abs(largest - PI / 2) < SAME -> AngleKind.RIGHT
        largest > PI / 2 -> AngleKind.OBTUSE
        else -> AngleKind.ACUTE
      }
    }

  val sideKind: SideKind
    get() {
      fun same(x: Double, y: Double) = abs(x - y) <= SAME * maxOf(x, y)
      val equalPairs = listOf(0 to 1, 1 to 2, 0 to 2).count { (i, j) -> same(sides[i], sides[j]) }
      return when (equalPairs) {
        0 -> SideKind.SCALENE
        3 -> SideKind.EQUILATERAL
        else -> SideKind.ISOSCELES
      }
    }
}

internal enum class AngleKind {
  ACUTE,
  RIGHT,
  OBTUSE,
}

internal enum class SideKind {
  SCALENE,
  ISOSCELES,
  EQUILATERAL,
}

/**
 * The triangles with the known [sides] and [angles] (radians), null for an unknown one. Three values
 * with at least one side are needed. Two sides and an angle that isn't between them can give two
 * triangles; values that don't fit give none.
 */
internal fun solveTriangle(sides: List<Double?>, angles: List<Double?>): List<Triangle> {
  val known = sides.count { it != null } + angles.count { it != null }
  val bad = sides.any { it != null && !(it > 0) } || angles.any { it != null && !(it > 0 && it < PI) }
  val filledAngles = if (known < NEEDED || sides.all { it == null } || bad) null else withThirdAngle(angles)
  return when {
    filledAngles == null -> emptyList()
    filledAngles.all { it != null } -> listOfNotNull(fromAngles(sides, filledAngles.map { it!! }))
    sides.all { it != null } -> listOfNotNull(fromSides(sides.map { it!! }))
    else -> fromTwoSides(sides, filledAngles)
  }
}

/** With two angles known, the third is what is left of 180°. Null when they leave nothing. */
private fun withThirdAngle(angles: List<Double?>): List<Double?>? {
  if (angles.count { it != null } != 2) return angles
  val rest = PI - angles.sumOf { it ?: 0.0 }
  if (rest <= 0) return null
  return angles.map { it ?: rest }
}

/** All angles and a side: the law of sines, a / sin A = b / sin B = c / sin C. */
private fun fromAngles(sides: List<Double?>, angles: List<Double>): Triangle? {
  if (abs(angles.sum() - PI) > SAME) return null
  val index = sides.indexOfFirst { it != null }
  val ratio = sides[index]!! / sin(angles[index])
  val all = angles.indices.map { sides[it] ?: (ratio * sin(angles[it])) }
  // Sides given with the angles must agree with them
  return Triangle(all, angles).takeIf { all.indices.all { i -> abs(all[i] - ratio * sin(angles[i])) <= SAME * all[i] } }
}

/** Three sides: the law of cosines for each angle, cos A = (b² + c² − a²) / (2bc). */
private fun fromSides(sides: List<Double>): Triangle? {
  val (a, b, c) = sides
  if (a + b <= c || a + c <= b || b + c <= a) return null
  fun angle(opposite: Double, x: Double, y: Double) = acos(((x * x + y * y - opposite * opposite) / (2 * x * y)).coerceIn(-1.0, 1.0))
  return Triangle(sides, listOf(angle(a, b, c), angle(b, a, c), angle(c, a, b)))
}

/**
 * Two sides and one angle. Between them (side, angle, side): the third side by the law of cosines.
 * Opposite one of them: the law of sines, with 0, 1 or 2 triangles.
 */
private fun fromTwoSides(sides: List<Double?>, angles: List<Double?>): List<Triangle> {
  val missing = sides.indexOfFirst { it == null }
  val angle = angles.indexOfFirst { it != null }
  return if (angle == missing) sideAngleSide(sides, angles[missing]!!) else sideSideAngle(sides, angles, angle)
}

/** The angle between the two known sides: the third side by the law of cosines. */
private fun sideAngleSide(sides: List<Double?>, between: Double): List<Triangle> {
  val (si, sj) = sides.filterNotNull()
  val third = sqrt(si * si + sj * sj - 2 * si * sj * cos(between))
  return listOfNotNull(fromSides(sides.map { it ?: third }))
}

/** The known [angle] is opposite one of the known sides: sin(other) = otherSide · sin(angle) / side. */
private fun sideSideAngle(sides: List<Double?>, angles: List<Double?>, angle: Int): List<Triangle> {
  val missing = sides.indexOfFirst { it == null }
  val other = (0..2).first { it != missing && it != angle }
  val sine = sides[other]!! * sin(angles[angle]!!) / sides[angle]!!
  val first = asin(sine.coerceAtMost(1.0))
  val candidates = if (sine > 1 + SAME) emptyList() else listOf(first, PI - first).distinctBy { (it / SAME).toLong() }
  return candidates.mapNotNull { otherAngle ->
    val third = PI - angles[angle]!! - otherAngle
    val known = angles.toMutableList().apply {
      this[other] = otherAngle
      this[missing] = third
    }
    if (third <= SAME) null else fromAngles(sides, known.map { it!! })
  }
}

/** Area, perimeter and diagonals of a flat shape, NaN where it has none. */
internal class Flat(val area: Double, val perimeter: Double, val diagonals: List<Double> = emptyList())

internal fun square(side: Double) = Flat(side * side, SQUARE_SIDES * side, listOf(side * sqrt(2.0)))

internal fun rectangle(width: Double, height: Double) =
  Flat(width * height, 2 * (width + height), listOf(hypot(width, height)))

/** Sides [base] and [side] with the [angle] between them, in radians. */
internal fun parallelogram(base: Double, side: Double, angle: Double) =
  Flat(
    base * side * sin(angle),
    2 * (base + side),
    listOf(sqrt(base * base + side * side + 2 * base * side * cos(angle)), sqrt(base * base + side * side - 2 * base * side * cos(angle))),
  )

internal fun rhombus(side: Double, angle: Double) = parallelogram(side, side, angle)

/**
 * The parallel sides [bottom] and [top], [height] between them, and the legs [left] and [right],
 * NaN when not given: no perimeter then.
 */
internal fun trapezoid(bottom: Double, top: Double, height: Double, left: Double, right: Double) =
  Flat((bottom + top) / 2 * height, bottom + top + left + right)

/** Diagonals [long] and [short], and its two pairs of equal sides, NaN when not given. */
internal fun kite(long: Double, short: Double, sideA: Double, sideB: Double) =
  Flat(long * short / 2, 2 * (sideA + sideB), listOf(long, short))

/**
 * A circle of [radius], and a sector of it with [angle] in radians: the arc, the sector, the chord
 * and the segment between the chord and the arc.
 */
internal class Circle(val radius: Double, val angle: Double = Double.NaN) {
  val diameter: Double
    get() = 2 * radius

  val circumference: Double
    get() = 2 * PI * radius

  val area: Double
    get() = PI * radius * radius

  val arc: Double
    get() = radius * angle

  val sectorArea: Double
    get() = radius * radius * angle / 2

  val chord: Double
    get() = 2 * radius * sin(angle / 2)

  val segmentArea: Double
    get() = radius * radius / 2 * (angle - sin(angle))
}

/** What a circle can be given by. */
internal enum class CircleGiven {
  RADIUS,
  DIAMETER,
  CIRCUMFERENCE,
  AREA,
}

/** The radius of a circle with [value] for [given]. */
internal fun radiusFrom(given: CircleGiven, value: Double): Double =
  when (given) {
    CircleGiven.RADIUS -> value
    CircleGiven.DIAMETER -> value / 2
    CircleGiven.CIRCUMFERENCE -> value / (2 * PI)
    CircleGiven.AREA -> sqrt(value / PI)
  }

/** Volume and outer surface of a solid; [lateral] without its bases, [slant] along its side, NaN when it has none. */
internal class Solid(val volume: Double, val surface: Double, val lateral: Double = Double.NaN, val slant: Double = Double.NaN)

internal fun cube(side: Double) = Solid(side * side * side, CUBE_FACES * side * side, SQUARE_SIDES * side * side)

internal fun cuboid(length: Double, width: Double, height: Double) =
  Solid(length * width * height, 2 * (length * width + length * height + width * height), 2 * (length + width) * height)

internal fun sphere(radius: Double) = Solid(SPHERE_VOLUME * PI * radius * radius * radius, SPHERE_SURFACE * PI * radius * radius)

internal fun cylinder(radius: Double, height: Double) =
  Solid(PI * radius * radius * height, 2 * PI * radius * (radius + height), 2 * PI * radius * height)

internal fun cone(radius: Double, height: Double): Solid {
  val slant = hypot(radius, height)
  return Solid(PI * radius * radius * height / POINTED, PI * radius * (radius + slant), PI * radius * slant, slant)
}

/** A pyramid on a square of [side], [height] high. Its slant is the height of a side face. */
internal fun squarePyramid(side: Double, height: Double): Solid {
  val slant = hypot(side / 2, height)
  return Solid(side * side * height / POINTED, side * side + 2 * side * slant, 2 * side * slant, slant)
}

/** Equal within rounding: 1e-9 of a turn or of a length. */
private const val SAME = 1e-9

/** Values that make a triangle: three, with a side. */
private const val NEEDED = 3
private const val SQUARE_SIDES = 4
private const val CUBE_FACES = 6

/** V = 4/3·π·r³, S = 4·π·r² */
private const val SPHERE_VOLUME = 4.0 / 3
private const val SPHERE_SURFACE = 4

/** A cone or a pyramid holds a third of the prism around it. */
private const val POINTED = 3
