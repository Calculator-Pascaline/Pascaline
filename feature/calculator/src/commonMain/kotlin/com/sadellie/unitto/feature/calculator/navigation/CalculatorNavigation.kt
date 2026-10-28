/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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

package com.sadellie.unitto.feature.calculator.navigation

import com.sadellie.unitto.core.navigation.CalculatorStartRoute
import com.sadellie.unitto.core.navigation.ElementsStartRoute
import com.sadellie.unitto.core.navigation.FinanceStartRoute
import com.sadellie.unitto.core.navigation.InferenceStartRoute
import com.sadellie.unitto.core.navigation.PricesStartRoute
import com.sadellie.unitto.core.navigation.FunctionsStartRoute
import com.sadellie.unitto.core.navigation.LocalNavigator
import com.sadellie.unitto.core.navigation.ProbabilityStartRoute
import com.sadellie.unitto.core.navigation.RegressionStartRoute
import com.sadellie.unitto.core.navigation.SequencesStartRoute
import com.sadellie.unitto.core.navigation.StatisticsStartRoute
import com.sadellie.unitto.feature.calculator.CalculatorRoute
import com.sadellie.unitto.feature.calculator.elements.ElementsRoute
import com.sadellie.unitto.feature.calculator.finance.FinanceRoute
import com.sadellie.unitto.feature.calculator.inference.InferenceRoute
import com.sadellie.unitto.feature.calculator.prices.PricesRoute
import com.sadellie.unitto.feature.calculator.functions.FunctionsRoute
import com.sadellie.unitto.feature.calculator.probability.ProbabilityRoute
import com.sadellie.unitto.feature.calculator.regression.RegressionRoute
import com.sadellie.unitto.feature.calculator.sequences.SequencesRoute
import com.sadellie.unitto.feature.calculator.statistics.StatisticsRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
fun Module.calculatorNavigation() {
  navigation<CalculatorStartRoute> {
    val navigator = LocalNavigator.current
    CalculatorRoute(openDrawer = navigator::openDrawer)
  }
  navigation<FunctionsStartRoute> {
    val navigator = LocalNavigator.current
    FunctionsRoute(openDrawer = navigator::openDrawer)
  }
  navigation<ProbabilityStartRoute> {
    val navigator = LocalNavigator.current
    ProbabilityRoute(openDrawer = navigator::openDrawer)
  }
  navigation<SequencesStartRoute> {
    val navigator = LocalNavigator.current
    SequencesRoute(openDrawer = navigator::openDrawer)
  }
  navigation<StatisticsStartRoute> {
    val navigator = LocalNavigator.current
    StatisticsRoute(openDrawer = navigator::openDrawer)
  }
  navigation<RegressionStartRoute> {
    val navigator = LocalNavigator.current
    RegressionRoute(openDrawer = navigator::openDrawer)
  }
  navigation<ElementsStartRoute> {
    val navigator = LocalNavigator.current
    ElementsRoute(openDrawer = navigator::openDrawer)
  }
  navigation<PricesStartRoute> {
    val navigator = LocalNavigator.current
    PricesRoute(openDrawer = navigator::openDrawer)
  }
  navigation<InferenceStartRoute> {
    val navigator = LocalNavigator.current
    InferenceRoute(openDrawer = navigator::openDrawer)
  }
  navigation<FinanceStartRoute> {
    val navigator = LocalNavigator.current
    FinanceRoute(openDrawer = navigator::openDrawer)
  }
}
