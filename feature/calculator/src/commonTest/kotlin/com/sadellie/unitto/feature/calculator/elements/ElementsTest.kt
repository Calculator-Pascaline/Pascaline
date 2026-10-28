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

package com.sadellie.unitto.feature.calculator.elements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ElementsTest {
  private fun element(symbol: String) = ELEMENTS.first { it.symbol == symbol }

  @Test fun all() = assertEquals(118, ELEMENTS.size)

  @Test fun numbers() = assertEquals((1..118).toList(), ELEMENTS.map { it.number })

  @Test fun uniqueSymbols() = assertEquals(118, ELEMENTS.map { it.symbol }.toSet().size)

  @Test fun lastOne() = assertEquals("Og", ELEMENTS.last().symbol)

  @Test fun iron() {
    val iron = element("Fe")
    assertEquals(26, iron.number)
    assertEquals(55.845, iron.mass, 1e-9)
    assertEquals(4, iron.period)
    assertEquals(8, iron.group)
    assertEquals(ElementFamily.TRANSITION_METAL, iron.family)
  }

  @Test fun groups() =
    assertEquals(
      listOf(1, 18, 1, 2, 13, 17, 18, 3, 12, 14, 3, 18),
      listOf("H", "He", "Li", "Be", "B", "F", "Ne", "Sc", "Zn", "Pb", "La", "Og").map { element(it).group },
    )

  @Test fun lanthanidesHaveNoGroup() = assertNull(element("Ce").group)

  @Test fun hafniumAfterLanthanides() = assertEquals(4, element("Hf").group)

  @Test
  fun positions() {
    assertEquals(0 to 0, element("H").tablePosition())
    assertEquals(0 to 17, element("He").tablePosition())
    assertEquals(5 to 2, element("La").tablePosition())
    // Ce starts the row of lanthanides under the table
    assertEquals(8 to 4, element("Ce").tablePosition())
    assertEquals(9 to 17, element("Lr").tablePosition())
  }

  @Test fun everyCellOnce() = assertEquals(118, ELEMENTS.map { it.tablePosition() }.toSet().size)

  @Test fun hydrogen() = assertEquals("1s¹", electronConfiguration(1))

  @Test fun oxygen() = assertEquals("[He] 2s² 2p⁴", electronConfiguration(8))

  @Test fun iron3d() = assertEquals("[Ar] 3d⁶ 4s²", electronConfiguration(26))

  @Test fun bromine() = assertEquals("[Ar] 3d¹⁰ 4s² 4p⁵", electronConfiguration(35))

  @Test fun chromium() = assertEquals("[Ar] 3d⁵ 4s¹", electronConfiguration(24))

  @Test fun lead() = assertEquals("[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p²", electronConfiguration(82))

  @Test fun neon() = assertEquals("[He] 2s² 2p⁶", electronConfiguration(10))

  // Masses grow with the atomic number, except for these well known pairs
  @Test
  fun massesGrow() =
    assertEquals(
      listOf("Ar" to "K", "Co" to "Ni", "Te" to "I", "Th" to "Pa", "U" to "Np", "Pu" to "Am", "Bh" to "Hs"),
      ELEMENTS.zipWithNext().filter { (a, b) -> b.mass < a.mass }.map { (a, b) -> a.symbol to b.symbol },
    )

  @Test fun fluorineMostElectronegative() = assertEquals("F", ELEMENTS.maxBy { it.electronegativity ?: 0.0 }.symbol)
}
