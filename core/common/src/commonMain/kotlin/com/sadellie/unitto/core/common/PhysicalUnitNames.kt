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

package com.sadellie.unitto.core.common

import org.jetbrains.compose.resources.StringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.electric_inductance
import unitto.core.common.generated.resources.physical_unit_acceleration_of_gravity
import unitto.core.common.generated.resources.physical_unit_ampere
import unitto.core.common.generated.resources.physical_unit_atmosphere
import unitto.core.common.generated.resources.physical_unit_avogadro_constant
import unitto.core.common.generated.resources.physical_unit_boltzmann_constant
import unitto.core.common.generated.resources.physical_unit_calorie
import unitto.core.common.generated.resources.physical_unit_candela
import unitto.core.common.generated.resources.physical_unit_coulomb
import unitto.core.common.generated.resources.physical_unit_coulomb_constant
import unitto.core.common.generated.resources.physical_unit_cup
import unitto.core.common.generated.resources.physical_unit_elementary_charge
import unitto.core.common.generated.resources.physical_unit_fluid_ounce
import unitto.core.common.generated.resources.physical_unit_gallon
import unitto.core.common.generated.resources.physical_unit_gigahertz
import unitto.core.common.generated.resources.physical_unit_gigawatt
import unitto.core.common.generated.resources.physical_unit_gradian
import unitto.core.common.generated.resources.physical_unit_gravitational_constant
import unitto.core.common.generated.resources.physical_unit_henry
import unitto.core.common.generated.resources.physical_unit_hertz
import unitto.core.common.generated.resources.physical_unit_kilocalorie
import unitto.core.common.generated.resources.physical_unit_kiloelectronvolt
import unitto.core.common.generated.resources.physical_unit_kilohertz
import unitto.core.common.generated.resources.physical_unit_kiloohm
import unitto.core.common.generated.resources.physical_unit_kilovolt
import unitto.core.common.generated.resources.physical_unit_long_ton
import unitto.core.common.generated.resources.physical_unit_megaelectronvolt
import unitto.core.common.generated.resources.physical_unit_megahertz
import unitto.core.common.generated.resources.physical_unit_megaohm
import unitto.core.common.generated.resources.physical_unit_microampere
import unitto.core.common.generated.resources.physical_unit_microhenry
import unitto.core.common.generated.resources.physical_unit_micromole
import unitto.core.common.generated.resources.physical_unit_microvolt
import unitto.core.common.generated.resources.physical_unit_microwatt
import unitto.core.common.generated.resources.physical_unit_milliampere
import unitto.core.common.generated.resources.physical_unit_millielectronvolt
import unitto.core.common.generated.resources.physical_unit_millihenry
import unitto.core.common.generated.resources.physical_unit_millijoule
import unitto.core.common.generated.resources.physical_unit_millimole
import unitto.core.common.generated.resources.physical_unit_millivolt
import unitto.core.common.generated.resources.physical_unit_milliwatt
import unitto.core.common.generated.resources.physical_unit_molar_gas_constant
import unitto.core.common.generated.resources.physical_unit_mole
import unitto.core.common.generated.resources.physical_unit_month
import unitto.core.common.generated.resources.physical_unit_nanohenry
import unitto.core.common.generated.resources.physical_unit_neutron_mass
import unitto.core.common.generated.resources.physical_unit_ohm
import unitto.core.common.generated.resources.physical_unit_picometer
import unitto.core.common.generated.resources.physical_unit_pint
import unitto.core.common.generated.resources.physical_unit_planck_constant
import unitto.core.common.generated.resources.physical_unit_proton_mass
import unitto.core.common.generated.resources.physical_unit_quart
import unitto.core.common.generated.resources.physical_unit_siemens
import unitto.core.common.generated.resources.physical_unit_speed_of_light
import unitto.core.common.generated.resources.physical_unit_tablespoon
import unitto.core.common.generated.resources.physical_unit_teaspoon
import unitto.core.common.generated.resources.physical_unit_tesla
import unitto.core.common.generated.resources.physical_unit_vacuum_permeability
import unitto.core.common.generated.resources.physical_unit_vacuum_permittivity
import unitto.core.common.generated.resources.physical_unit_volt
import unitto.core.common.generated.resources.physical_unit_year
import unitto.core.common.generated.resources.unit_acre
import unitto.core.common.generated.resources.unit_astronomical_unit
import unitto.core.common.generated.resources.unit_bar
import unitto.core.common.generated.resources.unit_celsius
import unitto.core.common.generated.resources.unit_centiliter
import unitto.core.common.generated.resources.unit_centimeter
import unitto.core.common.generated.resources.unit_cubic_centimeter
import unitto.core.common.generated.resources.unit_cubic_meter
import unitto.core.common.generated.resources.unit_day
import unitto.core.common.generated.resources.unit_deciliter
import unitto.core.common.generated.resources.unit_degree
import unitto.core.common.generated.resources.unit_electron_mass_rest
import unitto.core.common.generated.resources.unit_electron_volt
import unitto.core.common.generated.resources.unit_fahrenheit
import unitto.core.common.generated.resources.unit_farad
import unitto.core.common.generated.resources.unit_foot
import unitto.core.common.generated.resources.unit_gigawatt_hour
import unitto.core.common.generated.resources.unit_gram
import unitto.core.common.generated.resources.unit_group_angle
import unitto.core.common.generated.resources.unit_group_area
import unitto.core.common.generated.resources.unit_group_electrostatic_capacitance
import unitto.core.common.generated.resources.unit_group_energy
import unitto.core.common.generated.resources.unit_group_force
import unitto.core.common.generated.resources.unit_group_length
import unitto.core.common.generated.resources.unit_group_mass
import unitto.core.common.generated.resources.unit_group_power
import unitto.core.common.generated.resources.unit_group_pressure
import unitto.core.common.generated.resources.unit_group_speed
import unitto.core.common.generated.resources.unit_group_temperature
import unitto.core.common.generated.resources.unit_group_time
import unitto.core.common.generated.resources.unit_group_volume
import unitto.core.common.generated.resources.unit_hectare
import unitto.core.common.generated.resources.unit_hectopascal
import unitto.core.common.generated.resources.unit_horse_power_mechanical
import unitto.core.common.generated.resources.unit_hour
import unitto.core.common.generated.resources.unit_inch
import unitto.core.common.generated.resources.unit_joule
import unitto.core.common.generated.resources.unit_kelvin
import unitto.core.common.generated.resources.unit_kilogram
import unitto.core.common.generated.resources.unit_kilojoule
import unitto.core.common.generated.resources.unit_kilometer
import unitto.core.common.generated.resources.unit_kilometer_per_hour
import unitto.core.common.generated.resources.unit_kilonewton
import unitto.core.common.generated.resources.unit_kilopascal
import unitto.core.common.generated.resources.unit_kilowatt
import unitto.core.common.generated.resources.unit_kilowatt_hour
import unitto.core.common.generated.resources.unit_knot
import unitto.core.common.generated.resources.unit_light_year
import unitto.core.common.generated.resources.unit_liter
import unitto.core.common.generated.resources.unit_megapascal
import unitto.core.common.generated.resources.unit_megawatt
import unitto.core.common.generated.resources.unit_megawatt_hour
import unitto.core.common.generated.resources.unit_meter
import unitto.core.common.generated.resources.unit_meter_per_second
import unitto.core.common.generated.resources.unit_metric_ton
import unitto.core.common.generated.resources.unit_microfarad
import unitto.core.common.generated.resources.unit_microgram
import unitto.core.common.generated.resources.unit_micrometer
import unitto.core.common.generated.resources.unit_microsecond
import unitto.core.common.generated.resources.unit_mile
import unitto.core.common.generated.resources.unit_mile_per_hour
import unitto.core.common.generated.resources.unit_millifarad
import unitto.core.common.generated.resources.unit_milligram
import unitto.core.common.generated.resources.unit_milliliter
import unitto.core.common.generated.resources.unit_millimeter
import unitto.core.common.generated.resources.unit_millimeter_of_mercury
import unitto.core.common.generated.resources.unit_millinewton
import unitto.core.common.generated.resources.unit_millisecond
import unitto.core.common.generated.resources.unit_milliwatt_hour
import unitto.core.common.generated.resources.unit_minute
import unitto.core.common.generated.resources.unit_nanofarad
import unitto.core.common.generated.resources.unit_nanometer
import unitto.core.common.generated.resources.unit_nanosecond
import unitto.core.common.generated.resources.unit_newton
import unitto.core.common.generated.resources.unit_ounce
import unitto.core.common.generated.resources.unit_parsec
import unitto.core.common.generated.resources.unit_pascal
import unitto.core.common.generated.resources.unit_picofarad
import unitto.core.common.generated.resources.unit_pound
import unitto.core.common.generated.resources.unit_pound_force
import unitto.core.common.generated.resources.unit_psi
import unitto.core.common.generated.resources.unit_radian
import unitto.core.common.generated.resources.unit_second
import unitto.core.common.generated.resources.unit_short_ton
import unitto.core.common.generated.resources.unit_square_centimeter
import unitto.core.common.generated.resources.unit_square_foot
import unitto.core.common.generated.resources.unit_square_inch
import unitto.core.common.generated.resources.unit_square_kilometer
import unitto.core.common.generated.resources.unit_square_meter
import unitto.core.common.generated.resources.unit_square_mile
import unitto.core.common.generated.resources.unit_square_millimeter
import unitto.core.common.generated.resources.unit_square_yard
import unitto.core.common.generated.resources.unit_watt
import unitto.core.common.generated.resources.unit_watt_hour
import unitto.core.common.generated.resources.unit_week
import unitto.core.common.generated.resources.unit_yard
import unitto.core.common.generated.resources.units_menu_amount_of_substance
import unitto.core.common.generated.resources.units_menu_area_and_volume
import unitto.core.common.generated.resources.units_menu_astronomy
import unitto.core.common.generated.resources.units_menu_constants
import unitto.core.common.generated.resources.units_menu_current
import unitto.core.common.generated.resources.units_menu_electricity
import unitto.core.common.generated.resources.units_menu_energy_and_power
import unitto.core.common.generated.resources.units_menu_food
import unitto.core.common.generated.resources.units_menu_force_and_pressure
import unitto.core.common.generated.resources.units_menu_frequency
import unitto.core.common.generated.resources.units_menu_imperial
import unitto.core.common.generated.resources.units_menu_length_and_angle
import unitto.core.common.generated.resources.units_menu_other_units
import unitto.core.common.generated.resources.units_menu_particles
import unitto.core.common.generated.resources.units_menu_resistance
import unitto.core.common.generated.resources.units_menu_time_and_frequency
import unitto.core.common.generated.resources.units_menu_voltage

/**
 * Translated names of the units menu ([PhysicalUnits.menu]): its categories, units and constants.
 * The English names stay the keys, the menu's icons and colors are found by them.
 */
val physicalUnitNames: Map<String, StringResource> =
  mapOf(
    "Celsius" to Res.string.unit_celsius,
    "Fahrenheit" to Res.string.unit_fahrenheit,
    "Acceleration of gravity" to Res.string.physical_unit_acceleration_of_gravity,
    "Acre" to Res.string.unit_acre,
    "Ampere" to Res.string.physical_unit_ampere,
    "Astronomical unit" to Res.string.unit_astronomical_unit,
    "Atmosphere" to Res.string.physical_unit_atmosphere,
    "Avogadro constant" to Res.string.physical_unit_avogadro_constant,
    "Bar" to Res.string.unit_bar,
    "Boltzmann constant" to Res.string.physical_unit_boltzmann_constant,
    "Candela" to Res.string.physical_unit_candela,
    "Centiliter" to Res.string.unit_centiliter,
    "Centimeter" to Res.string.unit_centimeter,
    "Coulomb" to Res.string.physical_unit_coulomb,
    "Coulomb constant" to Res.string.physical_unit_coulomb_constant,
    "Cup" to Res.string.physical_unit_cup,
    "Day" to Res.string.unit_day,
    "Deciliter" to Res.string.unit_deciliter,
    "Degree" to Res.string.unit_degree,
    "Electron mass" to Res.string.unit_electron_mass_rest,
    "Electronvolt" to Res.string.unit_electron_volt,
    "Elementary charge" to Res.string.physical_unit_elementary_charge,
    "Farad" to Res.string.unit_farad,
    "Fluid ounce" to Res.string.physical_unit_fluid_ounce,
    "Foot" to Res.string.unit_foot,
    "Gallon" to Res.string.physical_unit_gallon,
    "Gigahertz" to Res.string.physical_unit_gigahertz,
    "Gigawatt" to Res.string.physical_unit_gigawatt,
    "Gigawatt-hour" to Res.string.unit_gigawatt_hour,
    "Gradian" to Res.string.physical_unit_gradian,
    "Gram" to Res.string.unit_gram,
    "Gravitational constant" to Res.string.physical_unit_gravitational_constant,
    "Hectare" to Res.string.unit_hectare,
    "Hectopascal" to Res.string.unit_hectopascal,
    "Henry" to Res.string.physical_unit_henry,
    "Square millimeter" to Res.string.unit_square_millimeter,
    "Square centimeter" to Res.string.unit_square_centimeter,
    "Square meter" to Res.string.unit_square_meter,
    "Square kilometer" to Res.string.unit_square_kilometer,
    "Square inch" to Res.string.unit_square_inch,
    "Square foot" to Res.string.unit_square_foot,
    "Square yard" to Res.string.unit_square_yard,
    "Square mile" to Res.string.unit_square_mile,
    "Cubic centimeter" to Res.string.unit_cubic_centimeter,
    "Cubic meter" to Res.string.unit_cubic_meter,
    "Meter/second" to Res.string.unit_meter_per_second,
    "Kilometer/hour" to Res.string.unit_kilometer_per_hour,
    "Mile/hour" to Res.string.unit_mile_per_hour,
    "Knot" to Res.string.unit_knot,
    "Pound-force" to Res.string.unit_pound_force,
    "Kilopascal" to Res.string.unit_kilopascal,
    "Megapascal" to Res.string.unit_megapascal,
    "Millimeter of mercury" to Res.string.unit_millimeter_of_mercury,
    "Pound/square inch" to Res.string.unit_psi,
    "Calorie" to Res.string.physical_unit_calorie,
    "Kilocalorie" to Res.string.physical_unit_kilocalorie,
    "Nanohenry" to Res.string.physical_unit_nanohenry,
    "Microhenry" to Res.string.physical_unit_microhenry,
    "Millihenry" to Res.string.physical_unit_millihenry,
    "Hertz" to Res.string.physical_unit_hertz,
    "Horsepower" to Res.string.unit_horse_power_mechanical,
    "Hour" to Res.string.unit_hour,
    "Inch" to Res.string.unit_inch,
    "Joule" to Res.string.unit_joule,
    "Kelvin" to Res.string.unit_kelvin,
    "Kiloelectronvolt" to Res.string.physical_unit_kiloelectronvolt,
    "Kilogram" to Res.string.unit_kilogram,
    "Kilohertz" to Res.string.physical_unit_kilohertz,
    "Kilojoule" to Res.string.unit_kilojoule,
    "Kilometer" to Res.string.unit_kilometer,
    "Kilonewton" to Res.string.unit_kilonewton,
    "Kiloohm" to Res.string.physical_unit_kiloohm,
    "Megaohm" to Res.string.physical_unit_megaohm,
    "Kilovolt" to Res.string.physical_unit_kilovolt,
    "Kilowatt" to Res.string.unit_kilowatt,
    "Kilowatt-hour" to Res.string.unit_kilowatt_hour,
    "Light-year" to Res.string.unit_light_year,
    "Liter" to Res.string.unit_liter,
    "Long ton" to Res.string.physical_unit_long_ton,
    "Megaelectronvolt" to Res.string.physical_unit_megaelectronvolt,
    "Megahertz" to Res.string.physical_unit_megahertz,
    "Megawatt" to Res.string.unit_megawatt,
    "Megawatt-hour" to Res.string.unit_megawatt_hour,
    "Meter" to Res.string.unit_meter,
    "Metric ton" to Res.string.unit_metric_ton,
    "Microampere" to Res.string.physical_unit_microampere,
    "Microfarad" to Res.string.unit_microfarad,
    "Nanofarad" to Res.string.unit_nanofarad,
    "Picofarad" to Res.string.unit_picofarad,
    "Microgram" to Res.string.unit_microgram,
    "Micrometer" to Res.string.unit_micrometer,
    "Micromole" to Res.string.physical_unit_micromole,
    "Microsecond" to Res.string.unit_microsecond,
    "Microvolt" to Res.string.physical_unit_microvolt,
    "Microwatt" to Res.string.physical_unit_microwatt,
    "Mile" to Res.string.unit_mile,
    "Milliampere" to Res.string.physical_unit_milliampere,
    "Millielectronvolt" to Res.string.physical_unit_millielectronvolt,
    "Millifarad" to Res.string.unit_millifarad,
    "Milligram" to Res.string.unit_milligram,
    "Millijoule" to Res.string.physical_unit_millijoule,
    "Milliliter" to Res.string.unit_milliliter,
    "Millimeter" to Res.string.unit_millimeter,
    "Millimole" to Res.string.physical_unit_millimole,
    "Millinewton" to Res.string.unit_millinewton,
    "Millisecond" to Res.string.unit_millisecond,
    "Millivolt" to Res.string.physical_unit_millivolt,
    "Milliwatt" to Res.string.physical_unit_milliwatt,
    "Milliwatt-hour" to Res.string.unit_milliwatt_hour,
    "Minute" to Res.string.unit_minute,
    "Molar gas constant" to Res.string.physical_unit_molar_gas_constant,
    "Mole" to Res.string.physical_unit_mole,
    "Month" to Res.string.physical_unit_month,
    "Nanometer" to Res.string.unit_nanometer,
    "Nanosecond" to Res.string.unit_nanosecond,
    "Neutron mass" to Res.string.physical_unit_neutron_mass,
    "Newton" to Res.string.unit_newton,
    "Ohm" to Res.string.physical_unit_ohm,
    "Ounce" to Res.string.unit_ounce,
    "Parsec" to Res.string.unit_parsec,
    "Pascal" to Res.string.unit_pascal,
    "Picometer" to Res.string.physical_unit_picometer,
    "Pint" to Res.string.physical_unit_pint,
    "Planck constant" to Res.string.physical_unit_planck_constant,
    "Pound" to Res.string.unit_pound,
    "Proton mass" to Res.string.physical_unit_proton_mass,
    "Quart" to Res.string.physical_unit_quart,
    "Radian" to Res.string.unit_radian,
    "Second" to Res.string.unit_second,
    "Short ton" to Res.string.unit_short_ton,
    "Siemens" to Res.string.physical_unit_siemens,
    "Speed of light" to Res.string.physical_unit_speed_of_light,
    "Tablespoon" to Res.string.physical_unit_tablespoon,
    "Teaspoon" to Res.string.physical_unit_teaspoon,
    "Tesla" to Res.string.physical_unit_tesla,
    "Vacuum permeability" to Res.string.physical_unit_vacuum_permeability,
    "Vacuum permittivity" to Res.string.physical_unit_vacuum_permittivity,
    "Volt" to Res.string.physical_unit_volt,
    "Watt" to Res.string.unit_watt,
    "Watt-hour" to Res.string.unit_watt_hour,
    "Week" to Res.string.unit_week,
    "Yard" to Res.string.unit_yard,
    "Year" to Res.string.physical_unit_year,
    "Length and angle" to Res.string.units_menu_length_and_angle,
    "Length" to Res.string.unit_group_length,
    "Imperial" to Res.string.units_menu_imperial,
    "Astronomy" to Res.string.units_menu_astronomy,
    "Angle" to Res.string.unit_group_angle,
    "Time and frequency" to Res.string.units_menu_time_and_frequency,
    "Time" to Res.string.unit_group_time,
    "Frequency" to Res.string.units_menu_frequency,
    "Area and volume" to Res.string.units_menu_area_and_volume,
    "Area" to Res.string.unit_group_area,
    "Volume" to Res.string.unit_group_volume,
    "Mass" to Res.string.unit_group_mass,
    "Electricity" to Res.string.units_menu_electricity,
    "Current" to Res.string.units_menu_current,
    "Voltage" to Res.string.units_menu_voltage,
    "Resistance" to Res.string.units_menu_resistance,
    "Capacitance" to Res.string.unit_group_electrostatic_capacitance,
    "Inductance" to Res.string.electric_inductance,
    "Speed" to Res.string.unit_group_speed,
    "Food" to Res.string.units_menu_food,
    "Particles" to Res.string.units_menu_particles,
    "Other units" to Res.string.units_menu_other_units,
    "Force and pressure" to Res.string.units_menu_force_and_pressure,
    "Force" to Res.string.unit_group_force,
    "Pressure" to Res.string.unit_group_pressure,
    "Energy and power" to Res.string.units_menu_energy_and_power,
    "Energy" to Res.string.unit_group_energy,
    "Power" to Res.string.unit_group_power,
    "Temperature" to Res.string.unit_group_temperature,
    "Amount of substance" to Res.string.units_menu_amount_of_substance,
    "Constants" to Res.string.units_menu_constants,
  )
