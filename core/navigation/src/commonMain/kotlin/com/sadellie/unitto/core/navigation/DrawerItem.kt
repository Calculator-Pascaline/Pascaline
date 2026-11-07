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

package com.sadellie.unitto.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.sadellie.unitto.core.designsystem.icons.symbols.AccessibilityNew
import com.sadellie.unitto.core.designsystem.icons.symbols.Atom
import com.sadellie.unitto.core.designsystem.icons.symbols.BarChart
import com.sadellie.unitto.core.designsystem.icons.symbols.BellCurve
import com.sadellie.unitto.core.designsystem.icons.symbols.BoxPlot
import com.sadellie.unitto.core.designsystem.icons.symbols.Calculate
import com.sadellie.unitto.core.designsystem.icons.symbols.CalculateFill
import com.sadellie.unitto.core.designsystem.icons.symbols.Charging
import com.sadellie.unitto.core.designsystem.icons.symbols.Code
import com.sadellie.unitto.core.designsystem.icons.symbols.Event
import com.sadellie.unitto.core.designsystem.icons.symbols.EventFill
import com.sadellie.unitto.core.designsystem.icons.symbols.Filters
import com.sadellie.unitto.core.designsystem.icons.symbols.Home
import com.sadellie.unitto.core.designsystem.icons.symbols.LineAxis
import com.sadellie.unitto.core.designsystem.icons.symbols.Logic
import com.sadellie.unitto.core.designsystem.icons.symbols.Orbits
import com.sadellie.unitto.core.designsystem.icons.symbols.Pendulum
import com.sadellie.unitto.core.designsystem.icons.symbols.Percent
import com.sadellie.unitto.core.designsystem.icons.symbols.Place
import com.sadellie.unitto.core.designsystem.icons.symbols.PriceTag
import com.sadellie.unitto.core.designsystem.icons.symbols.Projectile
import com.sadellie.unitto.core.designsystem.icons.symbols.Resistors
import com.sadellie.unitto.core.designsystem.icons.symbols.ScatterPlot
import com.sadellie.unitto.core.designsystem.icons.symbols.Schedule
import com.sadellie.unitto.core.designsystem.icons.symbols.ScheduleFill
import com.sadellie.unitto.core.designsystem.icons.symbols.Sequence
import com.sadellie.unitto.core.designsystem.icons.symbols.Settings
import com.sadellie.unitto.core.designsystem.icons.symbols.SettingsFill
import com.sadellie.unitto.core.designsystem.icons.symbols.SwapHoriz
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.designsystem.icons.symbols.Waves
import org.jetbrains.compose.resources.StringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.body_mass_title
import unitto.core.common.generated.resources.calculator_title
import unitto.core.common.generated.resources.charging_title
import unitto.core.common.generated.resources.converter_title
import unitto.core.common.generated.resources.date_calculator_title
import unitto.core.common.generated.resources.elements_title
import unitto.core.common.generated.resources.filters_title
import unitto.core.common.generated.resources.finance_title
import unitto.core.common.generated.resources.functions_title
import unitto.core.common.generated.resources.home_title
import unitto.core.common.generated.resources.inference_title
import unitto.core.common.generated.resources.logic_title
import unitto.core.common.generated.resources.orbits_title
import unitto.core.common.generated.resources.pendulum_title
import unitto.core.common.generated.resources.place_title
import unitto.core.common.generated.resources.prices_title
import unitto.core.common.generated.resources.probability_title
import unitto.core.common.generated.resources.programmer_title
import unitto.core.common.generated.resources.projectile_title
import unitto.core.common.generated.resources.regression_title
import unitto.core.common.generated.resources.resistors_title
import unitto.core.common.generated.resources.sequences_title
import unitto.core.common.generated.resources.settings_title
import unitto.core.common.generated.resources.statistics_title
import unitto.core.common.generated.resources.time_zone_title
import unitto.core.common.generated.resources.waves_title

sealed interface DrawerItem {
  val topLevelRoute: TopLevelRoute
  val name: StringResource
  val selectedIcon: ImageVector
  val defaultIcon: ImageVector
}

data object CalculatorDrawerItem : DrawerItem {
  override val topLevelRoute = CalculatorStartRoute
  override val name = Res.string.calculator_title
  override val selectedIcon = Symbols.CalculateFill
  override val defaultIcon = Symbols.Calculate
}

data object ConverterDrawerItem : DrawerItem {
  override val topLevelRoute = ConverterStartRoute()
  override val name = Res.string.converter_title
  override val selectedIcon = Symbols.SwapHoriz
  override val defaultIcon = Symbols.SwapHoriz
}

data object FunctionsDrawerItem : DrawerItem {
  override val topLevelRoute = FunctionsStartRoute
  override val name = Res.string.functions_title
  override val selectedIcon = Symbols.LineAxis
  override val defaultIcon = Symbols.LineAxis
}

data object ProbabilityDrawerItem : DrawerItem {
  override val topLevelRoute = ProbabilityStartRoute
  override val name = Res.string.probability_title
  override val selectedIcon = Symbols.BarChart
  override val defaultIcon = Symbols.BarChart
}

data object SequencesDrawerItem : DrawerItem {
  override val topLevelRoute = SequencesStartRoute
  override val name = Res.string.sequences_title
  override val selectedIcon = Symbols.Sequence
  override val defaultIcon = Symbols.Sequence
}

data object StatisticsDrawerItem : DrawerItem {
  override val topLevelRoute = StatisticsStartRoute
  override val name = Res.string.statistics_title
  override val selectedIcon = Symbols.BoxPlot
  override val defaultIcon = Symbols.BoxPlot
}

data object RegressionDrawerItem : DrawerItem {
  override val topLevelRoute = RegressionStartRoute
  override val name = Res.string.regression_title
  override val selectedIcon = Symbols.ScatterPlot
  override val defaultIcon = Symbols.ScatterPlot
}

data object ElementsDrawerItem : DrawerItem {
  override val topLevelRoute = ElementsStartRoute
  override val name = Res.string.elements_title
  override val selectedIcon = Symbols.Atom
  override val defaultIcon = Symbols.Atom
}

data object InferenceDrawerItem : DrawerItem {
  override val topLevelRoute = InferenceStartRoute
  override val name = Res.string.inference_title
  override val selectedIcon = Symbols.BellCurve
  override val defaultIcon = Symbols.BellCurve
}

data object FinanceDrawerItem : DrawerItem {
  override val topLevelRoute = FinanceStartRoute
  override val name = Res.string.finance_title
  override val selectedIcon = Symbols.Percent
  override val defaultIcon = Symbols.Percent
}

data object ProgrammerDrawerItem : DrawerItem {
  override val topLevelRoute = ProgrammerStartRoute
  override val name = Res.string.programmer_title
  override val selectedIcon = Symbols.Code
  override val defaultIcon = Symbols.Code
}

data object PricesDrawerItem : DrawerItem {
  override val topLevelRoute = PricesStartRoute
  override val name = Res.string.prices_title
  override val selectedIcon = Symbols.PriceTag
  override val defaultIcon = Symbols.PriceTag
}

data object DateCalculatorDrawerItem : DrawerItem {
  override val topLevelRoute = DateCalculatorStartRoute
  override val name = Res.string.date_calculator_title
  override val selectedIcon = Symbols.EventFill
  override val defaultIcon = Symbols.Event
}

data object TimeZonesDrawerItem : DrawerItem {
  override val topLevelRoute = TimeZoneStartRoute
  override val name = Res.string.time_zone_title
  override val selectedIcon = Symbols.ScheduleFill
  override val defaultIcon = Symbols.Schedule
}

data object BodyMassDrawerItem : DrawerItem {
  override val topLevelRoute = BodyMassStartRoute
  override val name = Res.string.body_mass_title
  override val selectedIcon = Symbols.AccessibilityNew
  override val defaultIcon = Symbols.AccessibilityNew
}

/** The grid of tools. Not a tool itself, so not in [mainDrawerItems]. */
data object HomeDrawerItem : DrawerItem {
  override val topLevelRoute = HomeStartRoute
  override val name = Res.string.home_title
  override val selectedIcon = Symbols.Home
  override val defaultIcon = Symbols.Home
}

data object ProjectileDrawerItem : DrawerItem {
  override val topLevelRoute = ProjectileStartRoute
  override val name = Res.string.projectile_title
  override val selectedIcon = Symbols.Projectile
  override val defaultIcon = Symbols.Projectile
}

data object PendulumDrawerItem : DrawerItem {
  override val topLevelRoute = PendulumStartRoute
  override val name = Res.string.pendulum_title
  override val selectedIcon = Symbols.Pendulum
  override val defaultIcon = Symbols.Pendulum
}

data object WavesDrawerItem : DrawerItem {
  override val topLevelRoute = WavesStartRoute
  override val name = Res.string.waves_title
  override val selectedIcon = Symbols.Waves
  override val defaultIcon = Symbols.Waves
}

data object OrbitsDrawerItem : DrawerItem {
  override val topLevelRoute = OrbitsStartRoute
  override val name = Res.string.orbits_title
  override val selectedIcon = Symbols.Orbits
  override val defaultIcon = Symbols.Orbits
}

data object FiltersDrawerItem : DrawerItem {
  override val topLevelRoute = FiltersStartRoute
  override val name = Res.string.filters_title
  override val selectedIcon = Symbols.Filters
  override val defaultIcon = Symbols.Filters
}

data object ResistorsDrawerItem : DrawerItem {
  override val topLevelRoute = ResistorsStartRoute
  override val name = Res.string.resistors_title
  override val selectedIcon = Symbols.Resistors
  override val defaultIcon = Symbols.Resistors
}

data object ChargingDrawerItem : DrawerItem {
  override val topLevelRoute = ChargingStartRoute
  override val name = Res.string.charging_title
  override val selectedIcon = Symbols.Charging
  override val defaultIcon = Symbols.Charging
}

data object LogicDrawerItem : DrawerItem {
  override val topLevelRoute = LogicStartRoute
  override val name = Res.string.logic_title
  override val selectedIcon = Symbols.Logic
  override val defaultIcon = Symbols.Logic
}

data object PlaceDrawerItem : DrawerItem {
  override val topLevelRoute = PlaceStartRoute
  override val name = Res.string.place_title
  override val selectedIcon = Symbols.Place
  override val defaultIcon = Symbols.Place
}

data object SettingsDrawerItem : DrawerItem {
  override val topLevelRoute = SettingsStartRoute
  override val name = Res.string.settings_title
  override val selectedIcon = Symbols.SettingsFill
  override val defaultIcon = Symbols.Settings
}

expect val mainDrawerItems: List<DrawerItem>

val additionalDrawerItems: List<DrawerItem> = listOf(SettingsDrawerItem)
