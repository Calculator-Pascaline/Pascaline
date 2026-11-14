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

package io.github.calculatorpascaline.feature.calculator.navigation

import io.github.calculatorpascaline.core.navigation.CalculatorStartRoute
import io.github.calculatorpascaline.core.navigation.ChangeoverStartRoute
import io.github.calculatorpascaline.core.navigation.ChargingStartRoute
import io.github.calculatorpascaline.core.navigation.ElementsStartRoute
import io.github.calculatorpascaline.core.navigation.FiltersStartRoute
import io.github.calculatorpascaline.core.navigation.FinanceStartRoute
import io.github.calculatorpascaline.core.navigation.FunctionsStartRoute
import io.github.calculatorpascaline.core.navigation.HomeStartRoute
import io.github.calculatorpascaline.core.navigation.InferenceStartRoute
import io.github.calculatorpascaline.core.navigation.InflationStartRoute
import io.github.calculatorpascaline.core.navigation.IntegralsStartRoute
import io.github.calculatorpascaline.core.navigation.LocalNavigator
import io.github.calculatorpascaline.core.navigation.LogicStartRoute
import io.github.calculatorpascaline.core.navigation.OrbitsStartRoute
import io.github.calculatorpascaline.core.navigation.PlaceStartRoute
import io.github.calculatorpascaline.core.navigation.PricesStartRoute
import io.github.calculatorpascaline.core.navigation.ProbabilityStartRoute
import io.github.calculatorpascaline.core.navigation.ProgrammerStartRoute
import io.github.calculatorpascaline.core.navigation.ProjectileStartRoute
import io.github.calculatorpascaline.core.navigation.RegressionStartRoute
import io.github.calculatorpascaline.core.navigation.ResistorsStartRoute
import io.github.calculatorpascaline.core.navigation.SequencesStartRoute
import io.github.calculatorpascaline.core.navigation.StatisticsStartRoute
import io.github.calculatorpascaline.core.navigation.SurfacesStartRoute
import io.github.calculatorpascaline.feature.calculator.CalculatorRoute
import io.github.calculatorpascaline.feature.calculator.electrical.ChargingRoute
import io.github.calculatorpascaline.feature.calculator.electrical.FiltersRoute
import io.github.calculatorpascaline.feature.calculator.electrical.LogicRoute
import io.github.calculatorpascaline.feature.calculator.electrical.ResistorsRoute
import io.github.calculatorpascaline.feature.calculator.elements.ElementsRoute
import io.github.calculatorpascaline.feature.calculator.finance.FinanceRoute
import io.github.calculatorpascaline.feature.calculator.functions.FunctionsRoute
import io.github.calculatorpascaline.feature.calculator.home.HomeRoute
import io.github.calculatorpascaline.feature.calculator.inference.InferenceRoute
import io.github.calculatorpascaline.feature.calculator.inflation.ChangeoverRoute
import io.github.calculatorpascaline.feature.calculator.inflation.InflationRoute
import io.github.calculatorpascaline.feature.calculator.integrals.IntegralsRoute
import io.github.calculatorpascaline.feature.calculator.physics.OrbitsRoute
import io.github.calculatorpascaline.feature.calculator.place.PlaceRoute
import io.github.calculatorpascaline.feature.calculator.prices.PricesRoute
import io.github.calculatorpascaline.feature.calculator.probability.ProbabilityRoute
import io.github.calculatorpascaline.feature.calculator.programmer.ProgrammerRoute
import io.github.calculatorpascaline.feature.calculator.projectile.ProjectileRoute
import io.github.calculatorpascaline.feature.calculator.regression.RegressionRoute
import io.github.calculatorpascaline.feature.calculator.sequences.SequencesRoute
import io.github.calculatorpascaline.feature.calculator.statistics.StatisticsRoute
import io.github.calculatorpascaline.feature.calculator.surfaces.SurfacesRoute
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
  navigation<ProgrammerStartRoute> {
    val navigator = LocalNavigator.current
    ProgrammerRoute(openDrawer = navigator::openDrawer)
  }
  navigation<HomeStartRoute> { HomeRoute() }
  navigation<ProjectileStartRoute> {
    val navigator = LocalNavigator.current
    ProjectileRoute(openDrawer = navigator::openDrawer)
  }
  navigation<OrbitsStartRoute> {
    val navigator = LocalNavigator.current
    OrbitsRoute(openDrawer = navigator::openDrawer)
  }
  navigation<FiltersStartRoute> {
    val navigator = LocalNavigator.current
    FiltersRoute(openDrawer = navigator::openDrawer)
  }
  navigation<ResistorsStartRoute> {
    val navigator = LocalNavigator.current
    ResistorsRoute(openDrawer = navigator::openDrawer)
  }
  navigation<ChargingStartRoute> {
    val navigator = LocalNavigator.current
    ChargingRoute(openDrawer = navigator::openDrawer)
  }
  navigation<LogicStartRoute> {
    val navigator = LocalNavigator.current
    LogicRoute(openDrawer = navigator::openDrawer)
  }
  navigation<IntegralsStartRoute> {
    val navigator = LocalNavigator.current
    IntegralsRoute(openDrawer = navigator::openDrawer)
  }
  navigation<InflationStartRoute> {
    val navigator = LocalNavigator.current
    InflationRoute(openDrawer = navigator::openDrawer)
  }
  navigation<ChangeoverStartRoute> {
    val navigator = LocalNavigator.current
    ChangeoverRoute(openDrawer = navigator::openDrawer)
  }
  navigation<SurfacesStartRoute> {
    val navigator = LocalNavigator.current
    SurfacesRoute(openDrawer = navigator::openDrawer)
  }
  navigation<PlaceStartRoute> {
    val navigator = LocalNavigator.current
    PlaceRoute(openDrawer = navigator::openDrawer)
  }
}
