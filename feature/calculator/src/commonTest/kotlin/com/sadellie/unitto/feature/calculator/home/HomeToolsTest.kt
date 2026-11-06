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

package com.sadellie.unitto.feature.calculator.home

import com.sadellie.unitto.core.navigation.ElementsDrawerItem
import com.sadellie.unitto.core.navigation.FinanceDrawerItem
import com.sadellie.unitto.core.navigation.NamedTool
import com.sadellie.unitto.core.navigation.ProjectileDrawerItem
import com.sadellie.unitto.core.navigation.alphabetical
import com.sadellie.unitto.core.navigation.ordered
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeToolsTest {
  private val tools =
    listOf(
      NamedTool(ProjectileDrawerItem, "Projectile"),
      NamedTool(FinanceDrawerItem, "Énergie"),
      NamedTool(ElementsDrawerItem, "éléments"),
    )

  private fun List<NamedTool>.names() = map { it.name }

  @Test
  fun alphabeticalIgnoresAccentsAndCapitals() =
    assertEquals(listOf("éléments", "Énergie", "Projectile"), tools.alphabetical().names())

  @Test
  fun savedOrderComesFirst_newToolsAfter() {
    val saved = tools[2].id + "," + tools[0].id
    assertEquals(listOf("éléments", "Projectile", "Énergie"), tools.ordered(saved).names())
    assertEquals(tools.names(), tools.ordered("").names())
    assertEquals(tools.names(), tools.ordered("gone").names())
  }

  @Test
  fun moved() {
    assertEquals(listOf("b", "c", "a"), listOf("a", "b", "c").moved(0, 2))
    assertEquals(listOf("c", "a", "b"), listOf("a", "b", "c").moved(2, 0))
  }
}
