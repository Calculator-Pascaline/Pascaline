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

/**
 * A section of Home.
 *
 * @property id Name of its topic for the first sections, made up for the ones that are added.
 * @property name What the user called it, null for the name of its topic.
 * @property tools Route ids of its tools, in their order.
 */
internal data class HomeSection(val id: String, val name: String?, val tools: List<String>) {
  /** One line: `id⇥name⇥tool,tool`. Tabs and new lines in a name would break it. */
  fun encode(): String = listOf(id, name.orEmpty().replace(Regex("[\\t\\n\\r]"), " "), tools.joinToString(TOOL_SEPARATOR)).joinToString(FIELD)

  companion object {
    fun decode(text: String): List<HomeSection>? =
      text
        .split(LINE)
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
          val parts = line.split(FIELD)
          if (parts.size < FIELDS) return@mapNotNull null
          HomeSection(parts[0], parts[1].takeIf { it.isNotBlank() }, parts[2].split(TOOL_SEPARATOR).filter { it.isNotBlank() })
        }
        .takeIf { it.isNotEmpty() }

    fun encode(sections: List<HomeSection>): String = sections.joinToString(LINE) { it.encode() }
  }
}

/**
 * The sections with the tools that are shown: [saved] when there are, [defaults] otherwise. A tool
 * is in one section only. Tools in none, like new ones, go to their section of [defaults] when it
 * is still there, or else to the last section.
 */
internal fun arrangeSections(saved: List<HomeSection>?, defaults: List<HomeSection>, shown: List<String>): List<HomeSection> {
  val placed = mutableSetOf<String>()
  val sections =
    (saved ?: defaults).map { section ->
      section.copy(tools = section.tools.filter { it in shown && placed.add(it) })
    }
  val missing = shown.filter { it !in placed }
  if (missing.isEmpty() || sections.isEmpty()) return sections
  return missing.fold(sections) { result, tool ->
    val home = defaults.firstOrNull { tool in it.tools }?.id
    val target = result.indexOfFirst { it.id == home }.takeIf { it >= 0 } ?: result.lastIndex
    result.mapIndexed { index, section -> if (index == target) section.copy(tools = section.tools + tool) else section }
  }
}

/** [tool] taken out of its section and put at the end of the section [to]. */
internal fun List<HomeSection>.withToolMoved(tool: String, to: String): List<HomeSection> = map { section ->
  val others = section.tools - tool
  if (section.id == to) section.copy(tools = others + tool) else section.copy(tools = others)
}

/** The section [id] one place up ([step] −1) or down (1). */
internal fun List<HomeSection>.withSectionMoved(id: String, step: Int): List<HomeSection> {
  val from = indexOfFirst { it.id == id }
  val to = from + step
  if (from < 0 || to !in indices) return this
  return toMutableList().also { list -> list.add(to, list.removeAt(from)) }
}

/** An id that no section has yet. */
internal fun List<HomeSection>.newSectionId(): String =
  generateSequence(1) { it + 1 }.map { "$CUSTOM_PREFIX$it" }.first { id -> none { it.id == id } }

private const val FIELD = "\t"
private const val LINE = "\n"
private const val TOOL_SEPARATOR = ","
private const val CUSTOM_PREFIX = "custom"
private const val FIELDS = 3
