/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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

package com.sadellie.unitto.core.model.calculator

/**
 * Calculation in history. [timestamp] is in milliseconds. An entry with a [label] is a favorite, it
 * asks before being deleted. [reused] are results of earlier calculations in the [expression].
 */
data class CalculatorHistoryItem(
  val id: Int,
  val expression: String,
  val result: String,
  val timestamp: Long = 0,
  val label: String? = null,
  val reused: List<ReusedResult> = emptyList(),
) {
  val isFavorite: Boolean
    get() = !label.isNullOrEmpty()
}

/**
 * Result of the calculation [sourceId], tapped in history and now the [length] characters from
 * [start] of an expression. Editing that calculation updates them.
 */
data class ReusedResult(val sourceId: Int, val start: Int, val length: Int) {
  val end: Int
    get() = start + length
}

/** `sourceId,start,length` of each result, `;` between them. Null when there are none. */
fun List<ReusedResult>.encode(): String? =
  ifEmpty { null }?.joinToString(";") { "${it.sourceId},${it.start},${it.length}" }

/** Reverse of [encode], parts that can't be read are left out. */
fun decodeReusedResults(encoded: String?): List<ReusedResult> =
  encoded.orEmpty().split(";").mapNotNull { part ->
    val numbers = part.split(",").mapNotNull { it.toIntOrNull() }
    numbers.takeIf { it.size == REUSED_RESULT_FIELDS }?.let { ReusedResult(it[0], it[1], it[2]) }
  }

private const val REUSED_RESULT_FIELDS = 3
