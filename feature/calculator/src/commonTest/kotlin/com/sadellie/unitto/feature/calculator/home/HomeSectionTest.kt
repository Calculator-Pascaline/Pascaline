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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeSectionTest {
  private val defaults =
    listOf(HomeSection("MATH", null, listOf("calc", "functions")), HomeSection("DATA", null, listOf("stats", "regression")))

  @Test
  fun savedAndReadBack() {
    val sections = listOf(HomeSection("MATH", "My math", listOf("calc")), HomeSection("custom1", null, emptyList()))
    assertEquals(sections, HomeSection.decode(HomeSection.encode(sections)))
    assertNull(HomeSection.decode(""))
  }

  @Test
  fun namesLoseTabsAndNewLines() {
    val text = HomeSection.encode(listOf(HomeSection("MATH", "a\tb\nc", listOf("calc"))))
    assertEquals("a b c", HomeSection.decode(text)?.single()?.name)
  }

  @Test
  fun defaultsWithoutSavedSections() =
    assertEquals(defaults, arrangeSections(null, defaults, listOf("calc", "functions", "stats", "regression")))

  @Test
  fun hiddenToolsAreLeftOut() =
    assertEquals(listOf("functions"), arrangeSections(null, defaults, listOf("functions", "stats", "regression"))[0].tools)

  @Test
  fun newToolGoesToItsTopicOrTheLastSection() {
    val saved = listOf(HomeSection("DATA", null, listOf("stats")), HomeSection("custom1", "Mine", listOf("calc")))
    val sections = arrangeSections(saved, defaults, listOf("calc", "functions", "stats", "regression"))
    // functions belongs to MATH, which was removed: last section. regression goes back to DATA.
    assertEquals(listOf("stats", "regression"), sections[0].tools)
    assertEquals(listOf("calc", "functions"), sections[1].tools)
  }

  @Test
  fun aToolIsInOneSectionOnly() {
    val saved = listOf(HomeSection("MATH", null, listOf("calc")), HomeSection("DATA", null, listOf("calc", "stats")))
    assertEquals(listOf("stats"), arrangeSections(saved, defaults, listOf("calc", "stats"))[1].tools)
  }

  @Test
  fun movingToolsAndSections() {
    val moved = defaults.withToolMoved("functions", "DATA")
    assertEquals(listOf("calc"), moved[0].tools)
    assertEquals(listOf("stats", "regression", "functions"), moved[1].tools)
    assertEquals(listOf("DATA", "MATH"), defaults.withSectionMoved("DATA", -1).map { it.id })
    assertEquals(defaults, defaults.withSectionMoved("MATH", -1))
    assertEquals("custom1", defaults.newSectionId())
  }
}
