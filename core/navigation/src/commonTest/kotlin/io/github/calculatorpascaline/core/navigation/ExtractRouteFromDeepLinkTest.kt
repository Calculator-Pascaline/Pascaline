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

package io.github.calculatorpascaline.core.navigation

import com.eygraber.uri.toKmpUri
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtractRouteFromDeepLinkTest {
  @Test fun test_empty() = assertExtractedRoute(null, "")

  @Test fun test_invalid() = assertExtractedRoute(null, "test")

  @Test fun test_invalid2() = assertExtractedRoute(null, "app://io.github.calculatorpascaline/")

  @Test
  fun test_calculatorStartRoute() =
    assertExtractedRoute(CalculatorStartRoute, "app://io.github.calculatorpascaline/calculator_route")

  @Test
  fun test_converterStartRoute1() =
    assertExtractedRoute(ConverterStartRoute(), "app://io.github.calculatorpascaline/converter_route")

  @Test
  fun test_converterStartRoute2_valid() =
    assertExtractedRoute(
      ConverterStartRoute("value1", "value2"),
      "app://io.github.calculatorpascaline/converter_route?unitFromId=value1&unitToId=value2",
    )

  @Test
  fun test_converterStartRoute2_invalid_emptyFirstParam() =
    assertExtractedRoute(
      ConverterStartRoute("", "value2"),
      "app://io.github.calculatorpascaline/converter_route?unitFromId=&unitToId=value2",
    )

  @Test
  fun test_converterStartRoute2_invalid_emptySecondParam() =
    assertExtractedRoute(
      ConverterStartRoute("value1", ""),
      "app://io.github.calculatorpascaline/converter_route?unitFromId=value1&unitToId=",
    )

  @Test
  fun test_dateCalculatorStartRoute() =
    assertExtractedRoute(
      DateCalculatorStartRoute,
      "app://io.github.calculatorpascaline/date_calculator_route",
    )

  @Test
  fun test_homeStartRoute() = assertExtractedRoute(HomeStartRoute, "app://io.github.calculatorpascaline/home_route")

  @Test
  fun test_projectileStartRoute() =
    assertExtractedRoute(ProjectileStartRoute, "app://io.github.calculatorpascaline/projectile_route")

  @Test
  fun test_orbitsStartRoute() =
    assertExtractedRoute(OrbitsStartRoute, "app://io.github.calculatorpascaline/orbits_route")

  @Test
  fun test_surfacesStartRoute() =
    assertExtractedRoute(SurfacesStartRoute, "app://io.github.calculatorpascaline/surfaces_route")

  @Test
  fun test_changeoverStartRoute() =
    assertExtractedRoute(ChangeoverStartRoute, "app://io.github.calculatorpascaline/changeover_route")

  @Test
  fun test_inflationStartRoute() =
    assertExtractedRoute(InflationStartRoute, "app://io.github.calculatorpascaline/inflation_route")

  @Test
  fun test_integralsStartRoute() =
    assertExtractedRoute(IntegralsStartRoute, "app://io.github.calculatorpascaline/integrals_route")

  @Test
  fun test_gearsStartRoute() =
    assertExtractedRoute(GearsStartRoute, "app://io.github.calculatorpascaline/gears_route")

  @Test
  fun test_filtersStartRoute() =
    assertExtractedRoute(FiltersStartRoute, "app://io.github.calculatorpascaline/filters_route")

  @Test
  fun test_resistorsStartRoute() =
    assertExtractedRoute(ResistorsStartRoute, "app://io.github.calculatorpascaline/resistors_route")

  @Test
  fun test_chargingStartRoute() =
    assertExtractedRoute(ChargingStartRoute, "app://io.github.calculatorpascaline/charging_route")

  @Test
  fun test_logicStartRoute() =
    assertExtractedRoute(LogicStartRoute, "app://io.github.calculatorpascaline/logic_route")

  @Test
  fun test_placeStartRoute() =
    assertExtractedRoute(PlaceStartRoute, "app://io.github.calculatorpascaline/place_route")

  @Test
  fun test_bodyMassStartRoute() =
    assertExtractedRoute(BodyMassStartRoute, "app://io.github.calculatorpascaline/body_mass_route")

  @Test
  fun test_legacyHost() =
    assertExtractedRoute(
      ConverterStartRoute("value1", "value2"),
      "app://com.sadellie.unitto/converter_route?unitFromId=value1&unitToId=value2",
    )

  private fun assertExtractedRoute(expected: Route?, uriString: String) =
    assertEquals(expected, Route.extractRouteFromDeeplink(uriString.toKmpUri()))
}
