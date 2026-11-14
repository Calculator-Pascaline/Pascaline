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

/** A tool with its name in the language of the app. */
class NamedTool(val item: DrawerItem, val name: String) {
  val id = item.topLevelRoute.routeId
}

/** Tools from A to Z, ignoring accents and capitals. */
fun List<NamedTool>.alphabetical(): List<NamedTool> = sortedBy { sortKey(it.name) }

/** The most used tools first, in the order of [mainDrawerItems]. */
fun List<NamedTool>.inDefaultOrder(): List<NamedTool> = sortedBy { mainDrawerItems.indexOf(it.item) }

/** Tools in the [saved] order, the ones that aren't in it stay as they were after them. */
fun List<NamedTool>.ordered(saved: String): List<NamedTool> {
  val first = saved.split(ORDER_SEPARATOR).mapNotNull { id -> firstOrNull { it.id == id } }
  return first + (this - first.toSet())
}

private fun sortKey(text: String) =
  text.lowercase().map { character -> ACCENTS.entries.firstOrNull { character in it.key }?.value ?: character }.joinToString("")

/** Key of the order of the tools in app data, ids separated by [ORDER_SEPARATOR]. */
const val TOOL_ORDER_KEY = "HOME_ORDER"
const val ORDER_SEPARATOR = ","

/** Key of the tool that was open last in app data, to open it again. Empty when it was Home. */
const val LAST_TOOL_KEY = "LAST_TOOL"

/** The tool saved under [LAST_TOOL_KEY], null for Home, the settings or a tool that is gone. */
fun lastTool(saved: String): TopLevelRoute? = mainDrawerItems.firstOrNull { it.topLevelRoute.routeId == saved }?.topLevelRoute

private val ACCENTS =
  mapOf("àáâãäå" to 'a', "ç" to 'c', "èéêë" to 'e', "ìíîï" to 'i', "ñ" to 'n', "òóôõöő" to 'o', "ùúûüű" to 'u', "ş" to 's', "ğ" to 'g', "ı" to 'i')
