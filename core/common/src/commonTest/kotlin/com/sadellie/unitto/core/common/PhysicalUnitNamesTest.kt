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

package com.sadellie.unitto.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

/** A category, unit or constant of the units menu without a translated name is shown in English. */
class PhysicalUnitNamesTest {
  @Test
  fun everyNameOfTheMenuIsTranslated() {
    fun names(items: List<UnitMenuItem>): List<String> =
      items.flatMap {
        when (it) {
          is UnitMenuItem.Category -> listOf(it.name) + names(it.children)
          is UnitMenuItem.Leaf -> listOf(it.unit.name)
        }
      }
    assertEquals(emptyList(), names(PhysicalUnits.menu).distinct().filter { it !in physicalUnitNames })
  }
}
