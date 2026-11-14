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

package io.github.calculatorpascaline.feature.converter.navigation

import io.github.calculatorpascaline.core.model.converter.UnitGroup
import io.github.calculatorpascaline.core.navigation.ConverterStartRoute
import io.github.calculatorpascaline.core.navigation.LocalEventBus
import io.github.calculatorpascaline.core.navigation.LocalNavigator
import io.github.calculatorpascaline.core.navigation.ResultEffect
import io.github.calculatorpascaline.core.navigation.Route
import io.github.calculatorpascaline.feature.converter.ConverterRoute
import io.github.calculatorpascaline.feature.converter.ConverterViewModel
import io.github.calculatorpascaline.feature.converter.UnitFromSelectorRoute
import io.github.calculatorpascaline.feature.converter.UnitToSelectorRoute
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
fun Module.converterNavigation() {
  navigation<ConverterStartRoute> {
    val viewModel = koinViewModel<ConverterViewModel> { parametersOf(it) }
    val resultEventBus = LocalEventBus.current
    val navigator = LocalNavigator.current
    ResultEffect<String>(resultEventBus, RESULT_TAG_FROM) { unitFromId ->
      viewModel.updateUnitFromId(unitFromId)
    }
    ResultEffect<String>(resultEventBus, RESULT_TAG_TO) { unitToId ->
      viewModel.updateUnitToId(unitToId)
    }
    ConverterRoute(
      viewModel = viewModel,
      navigateToLeftScreen = { unitFromId, group ->
        navigator.goTo(UnitFromRoute(unitFromId, group))
      },
      navigateToRightScreen = { unitFromId, unitToId, group, input1, input2 ->
        navigator.goTo(UnitToRoute(unitFromId, unitToId, group, input1, input2))
      },
      openDrawer = navigator::openDrawer,
    )
  }
  navigation<UnitFromRoute> {
    val resultEventBus = LocalEventBus.current
    val navigator = LocalNavigator.current
    UnitFromSelectorRoute(
      unitSelectorViewModel = koinViewModel { parametersOf(it) },
      updateUnitFrom = { unitFromId -> resultEventBus.sendResult(RESULT_TAG_FROM, unitFromId) },
      navigateUp = navigator::goBack,
      navigateToUnitGroups = navigator::navigateToUnitGroups,
    )
  }
  navigation<UnitToRoute> {
    val resultEventBus = LocalEventBus.current
    val navigator = LocalNavigator.current
    UnitToSelectorRoute(
      unitSelectorViewModel = koinViewModel { parametersOf(it) },
      updateUnitTo = { unitToId -> resultEventBus.sendResult(RESULT_TAG_TO, unitToId) },
      navigateUp = navigator::goBack,
      navigateToUnitGroups = navigator::navigateToUnitGroups,
    )
  }
}

@Serializable
data class UnitFromRoute(val unitFromId: String, val unitGroup: UnitGroup) : Route {
  override val routeId = "unit_from"
}

@Serializable
data class UnitToRoute(
  val unitFromId: String,
  val unitToId: String,
  val unitGroup: UnitGroup,
  val input1: String,
  val input2: String,
) : Route {
  override val routeId = "unit_to"
}

private const val RESULT_TAG_FROM = "from"
private const val RESULT_TAG_TO = "to"
