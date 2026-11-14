/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025 Elshan Agaev
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

package io.github.calculatorpascaline.feature.datecalculator.navigation

import io.github.calculatorpascaline.core.navigation.DateCalculatorStartRoute
import io.github.calculatorpascaline.core.navigation.LocalNavigator
import io.github.calculatorpascaline.feature.datecalculator.DateCalculatorRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
internal fun Module.dateCalculatorNavigation() {
  navigation<DateCalculatorStartRoute> {
    val navigator = LocalNavigator.current
    DateCalculatorRoute(openDrawer = navigator::openDrawer)
  }
}
