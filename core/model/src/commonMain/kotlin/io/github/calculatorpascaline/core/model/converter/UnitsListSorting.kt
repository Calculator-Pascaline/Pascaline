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

package io.github.calculatorpascaline.core.model.converter

import org.jetbrains.compose.resources.StringResource
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.settings_sort_by_alphabetical
import pascaline.core.common.generated.resources.settings_sort_by_scale_asc
import pascaline.core.common.generated.resources.settings_sort_by_scale_desc
import pascaline.core.common.generated.resources.settings_sort_by_usage

enum class UnitsListSorting(val res: StringResource) {
  USAGE(Res.string.settings_sort_by_usage),
  ALPHABETICAL(Res.string.settings_sort_by_alphabetical),
  SCALE_DESC(Res.string.settings_sort_by_scale_desc),
  SCALE_ASC(Res.string.settings_sort_by_scale_asc),
}
