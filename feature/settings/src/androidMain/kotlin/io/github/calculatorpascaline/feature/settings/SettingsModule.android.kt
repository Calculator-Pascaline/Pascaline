/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025-2026 Elshan Agaev
 * Copyright (c) 2026 Calculator-Pascaline (modifications)
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

package io.github.calculatorpascaline.feature.settings

import io.github.calculatorpascaline.feature.settings.backup.BackupViewModel
import io.github.calculatorpascaline.feature.settings.calculator.CalculatorSettingsViewModel
import io.github.calculatorpascaline.feature.settings.converter.ConverterSettingsViewModel
import io.github.calculatorpascaline.feature.settings.display.DisplayViewModel
import io.github.calculatorpascaline.feature.settings.formatting.FormattingViewModel
import io.github.calculatorpascaline.feature.settings.navigation.settingNavigation
import io.github.calculatorpascaline.feature.settings.unitgroups.UnitGroupsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.lazyModule

val settingsModule = lazyModule {
  settingNavigation()
  viewModelOf(::BackupViewModel)
  viewModelOf(::CalculatorSettingsViewModel)
  viewModelOf(::ConverterSettingsViewModel)
  viewModelOf(::DisplayViewModel)
  viewModelOf(::FormattingViewModel)
  viewModelOf(::UnitGroupsViewModel)
  viewModelOf(::SettingsViewModel)
}
