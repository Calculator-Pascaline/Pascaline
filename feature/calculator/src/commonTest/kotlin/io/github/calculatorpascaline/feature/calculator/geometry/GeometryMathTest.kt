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
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun deg(value: Double) = value * PI / 180

class GeometryMathTest {
  // 3, 4, 5: a right triangle with an area of 6
  @Test
  fun threeSides() {
    val t = solveTriangle(listOf(3.0, 4.0, 5.0), listOf(null, null, null)).single()
    assertEquals(90.0, t.angles[2] * 180 / PI, 1e-9)
    assertEquals(6.0, t.area, 1e-9)
    assertEquals(12.0, t.perimeter, 1e-9)
    assertEquals(1.0, t.inradius, 1e-9)
    assertEquals(2.5, t.circumradius, 1e-9)
    assertEquals(AngleKind.RIGHT, t.angleKind)
    assertEquals(SideKind.SCALENE, t.sideKind)
  }

  @Test fun sidesThatDontClose() = assertTrue(solveTriangle(listOf(1.0, 2.0, 3.0), listOf(null, null, null)).isEmpty())

  // Side, angle, side: 3 and 4 with 90° between them
  @Test
  fun twoSidesAndTheAngleBetween() {
    val t = solveTriangle(listOf(3.0, 4.0, null), listOf(null, null, deg(90.0))).single()
    assertEquals(5.0, t.sides[2], 1e-9)
  }

  // Angle, side, angle: 60°, 60° and a side of 2 is equilateral
  @Test
  fun twoAnglesAndASide() {
    val t = solveTriangle(listOf(2.0, null, null), listOf(deg(60.0), deg(60.0), null)).single()
    assertEquals(listOf(2.0, 2.0, 2.0), t.sides.map { kotlin.math.round(it * 1e9) / 1e9 })
    assertEquals(SideKind.EQUILATERAL, t.sideKind)
    assertEquals(sqrt(3.0), t.area, 1e-9)
  }

  // Side, side, angle: a = 6, b = 8, A = 30° fits two triangles
  @Test
  fun ambiguousCaseGivesTwo() {
    val found = solveTriangle(listOf(6.0, 8.0, null), listOf(deg(30.0), null, null))
    assertEquals(2, found.size)
    assertEquals(41.81, found[0].angles[1] * 180 / PI, 0.01)
    assertEquals(138.19, found[1].angles[1] * 180 / PI, 0.01)
  }

  @Test fun sideTooShortForTheAngle() = assertTrue(solveTriangle(listOf(2.0, 8.0, null), listOf(deg(30.0), null, null)).isEmpty())

  @Test fun anglesWithoutASide() = assertTrue(solveTriangle(listOf(null, null, null), listOf(deg(60.0), deg(60.0), deg(60.0))).isEmpty())

  @Test fun anglesOver180() = assertTrue(solveTriangle(listOf(1.0, null, null), listOf(deg(100.0), deg(90.0), null)).isEmpty())

  @Test fun twoValuesAreNotEnough() = assertTrue(solveTriangle(listOf(3.0, 4.0, null), listOf(null, null, null)).isEmpty())

  @Test
  fun parallelogramWithARightAngleIsARectangle() {
    val shape = parallelogram(4.0, 3.0, PI / 2)
    assertEquals(12.0, shape.area, 1e-9)
    assertEquals(listOf(5.0, 5.0), shape.diagonals.map { kotlin.math.round(it * 1e9) / 1e9 })
  }

  @Test fun trapezoidArea() = assertEquals(15.0, trapezoid(6.0, 4.0, 3.0, Double.NaN, Double.NaN).area)

  @Test fun kiteArea() = assertEquals(12.0, kite(6.0, 4.0, 3.0, 5.0).area)

  @Test
  fun circleFromItsArea() {
    val circle = Circle(radiusFrom(CircleGiven.AREA, PI * 9), PI / 2)
    assertEquals(3.0, circle.radius, 1e-9)
    assertEquals(3.0 * PI / 2, circle.arc, 1e-9)
    assertEquals(3.0 * sqrt(2.0), circle.chord, 1e-9)
    assertEquals(9 * PI / 4, circle.sectorArea, 1e-9)
  }

  @Test fun sphereVolume() = assertEquals(4.0 / 3 * PI * 8, sphere(2.0).volume, 1e-9)

  // Cone of radius 3 and height 4: slant 5, surface π·3·(3 + 5)
  @Test
  fun cone() {
    val solid = cone(3.0, 4.0)
    assertEquals(5.0, solid.slant, 1e-9)
    assertEquals(24 * PI, solid.surface, 1e-9)
    assertEquals(12 * PI, solid.volume, 1e-9)
  }

  @Test fun pyramidVolume() = assertEquals(48.0, squarePyramid(6.0, 4.0).volume, 1e-9)
}
