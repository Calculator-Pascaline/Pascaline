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

package com.sadellie.unitto.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class DrawerLayoutTest {
  private val items = listOf(CalculatorDrawerItem, FunctionsDrawerItem, ConverterDrawerItem)

  @Test
  fun nothingSaved_showsEverything() =
    assertEquals(DrawerLayout(items, emptyList()), DrawerLayout.parse("", items))

  @Test
  fun savedLayout_roundTrips() {
    val layout = DrawerLayout(listOf(ConverterDrawerItem, CalculatorDrawerItem), listOf(FunctionsDrawerItem))
    assertEquals(layout, DrawerLayout.parse(layout.save(), items))
  }

  @Test
  fun newApps_areShownAtTheEnd() =
    assertEquals(
      DrawerLayout(listOf(ConverterDrawerItem, CalculatorDrawerItem), emptyList()),
      DrawerLayout.parse("converter_route;", listOf(CalculatorDrawerItem, ConverterDrawerItem)),
    )

  @Test
  fun unknownIds_areIgnored() =
    assertEquals(
      DrawerLayout(listOf(FunctionsDrawerItem, CalculatorDrawerItem, ConverterDrawerItem), emptyList()),
      DrawerLayout.parse("removed_route,functions_route;also_removed", items),
    )

  @Test
  fun everythingHidden_showsEverything() =
    assertEquals(
      DrawerLayout(items, emptyList()),
      DrawerLayout.parse(";calculator_route,functions_route,converter_route", items),
    )
}
