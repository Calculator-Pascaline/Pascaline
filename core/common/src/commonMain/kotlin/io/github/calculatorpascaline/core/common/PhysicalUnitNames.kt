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

package io.github.calculatorpascaline.core.common

import org.jetbrains.compose.resources.StringResource
import pascaline.core.common.generated.resources.physical_unit_reduced_planck_constant
import pascaline.core.common.generated.resources.physical_unit_stefan_boltzmann_constant
import pascaline.core.common.generated.resources.physical_unit_bohr_radius
import pascaline.core.common.generated.resources.physical_unit_atomic_mass_constant
import pascaline.core.common.generated.resources.physical_unit_molar_volume
import pascaline.core.common.generated.resources.units_menu_fundamental
import pascaline.core.common.generated.resources.units_menu_gravity
import pascaline.core.common.generated.resources.units_menu_chemistry_and_heat
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.electric_inductance
import pascaline.core.common.generated.resources.physical_unit_acceleration_of_gravity
import pascaline.core.common.generated.resources.physical_unit_ampere
import pascaline.core.common.generated.resources.physical_unit_atmosphere
import pascaline.core.common.generated.resources.physical_unit_avogadro_constant
import pascaline.core.common.generated.resources.physical_unit_boltzmann_constant
import pascaline.core.common.generated.resources.physical_unit_calorie
import pascaline.core.common.generated.resources.physical_unit_candela
import pascaline.core.common.generated.resources.physical_unit_coulomb
import pascaline.core.common.generated.resources.physical_unit_coulomb_constant
import pascaline.core.common.generated.resources.physical_unit_cup
import pascaline.core.common.generated.resources.physical_unit_elementary_charge
import pascaline.core.common.generated.resources.physical_unit_fluid_ounce
import pascaline.core.common.generated.resources.physical_unit_gallon
import pascaline.core.common.generated.resources.physical_unit_gigahertz
import pascaline.core.common.generated.resources.physical_unit_gigawatt
import pascaline.core.common.generated.resources.physical_unit_gradian
import pascaline.core.common.generated.resources.physical_unit_gravitational_constant
import pascaline.core.common.generated.resources.physical_unit_henry
import pascaline.core.common.generated.resources.physical_unit_hertz
import pascaline.core.common.generated.resources.physical_unit_kilocalorie
import pascaline.core.common.generated.resources.physical_unit_kiloelectronvolt
import pascaline.core.common.generated.resources.physical_unit_kilohertz
import pascaline.core.common.generated.resources.physical_unit_kiloohm
import pascaline.core.common.generated.resources.physical_unit_kilovolt
import pascaline.core.common.generated.resources.physical_unit_long_ton
import pascaline.core.common.generated.resources.physical_unit_megaelectronvolt
import pascaline.core.common.generated.resources.physical_unit_megahertz
import pascaline.core.common.generated.resources.physical_unit_megaohm
import pascaline.core.common.generated.resources.physical_unit_microampere
import pascaline.core.common.generated.resources.physical_unit_microhenry
import pascaline.core.common.generated.resources.physical_unit_micromole
import pascaline.core.common.generated.resources.physical_unit_microvolt
import pascaline.core.common.generated.resources.physical_unit_microwatt
import pascaline.core.common.generated.resources.physical_unit_milliampere
import pascaline.core.common.generated.resources.physical_unit_millielectronvolt
import pascaline.core.common.generated.resources.physical_unit_millihenry
import pascaline.core.common.generated.resources.physical_unit_millijoule
import pascaline.core.common.generated.resources.physical_unit_millimole
import pascaline.core.common.generated.resources.physical_unit_millivolt
import pascaline.core.common.generated.resources.physical_unit_milliwatt
import pascaline.core.common.generated.resources.physical_unit_molar_gas_constant
import pascaline.core.common.generated.resources.physical_unit_mole
import pascaline.core.common.generated.resources.physical_unit_month
import pascaline.core.common.generated.resources.physical_unit_nanohenry
import pascaline.core.common.generated.resources.physical_unit_neutron_mass
import pascaline.core.common.generated.resources.physical_unit_ohm
import pascaline.core.common.generated.resources.physical_unit_picometer
import pascaline.core.common.generated.resources.physical_unit_pint
import pascaline.core.common.generated.resources.physical_unit_planck_constant
import pascaline.core.common.generated.resources.physical_unit_proton_mass
import pascaline.core.common.generated.resources.physical_unit_quart
import pascaline.core.common.generated.resources.physical_unit_siemens
import pascaline.core.common.generated.resources.physical_unit_speed_of_light
import pascaline.core.common.generated.resources.physical_unit_tablespoon
import pascaline.core.common.generated.resources.physical_unit_teaspoon
import pascaline.core.common.generated.resources.physical_unit_tesla
import pascaline.core.common.generated.resources.physical_unit_vacuum_permeability
import pascaline.core.common.generated.resources.physical_unit_vacuum_permittivity
import pascaline.core.common.generated.resources.physical_unit_volt
import pascaline.core.common.generated.resources.physical_unit_year
import pascaline.core.common.generated.resources.unit_acre
import pascaline.core.common.generated.resources.unit_astronomical_unit
import pascaline.core.common.generated.resources.unit_bar
import pascaline.core.common.generated.resources.unit_celsius
import pascaline.core.common.generated.resources.unit_centiliter
import pascaline.core.common.generated.resources.unit_centimeter
import pascaline.core.common.generated.resources.unit_cubic_centimeter
import pascaline.core.common.generated.resources.unit_cubic_meter
import pascaline.core.common.generated.resources.unit_day
import pascaline.core.common.generated.resources.unit_deciliter
import pascaline.core.common.generated.resources.unit_degree
import pascaline.core.common.generated.resources.unit_electron_mass_rest
import pascaline.core.common.generated.resources.unit_electron_volt
import pascaline.core.common.generated.resources.unit_fahrenheit
import pascaline.core.common.generated.resources.unit_farad
import pascaline.core.common.generated.resources.unit_foot
import pascaline.core.common.generated.resources.unit_gigawatt_hour
import pascaline.core.common.generated.resources.unit_gram
import pascaline.core.common.generated.resources.unit_group_angle
import pascaline.core.common.generated.resources.unit_group_area
import pascaline.core.common.generated.resources.unit_group_electrostatic_capacitance
import pascaline.core.common.generated.resources.unit_group_energy
import pascaline.core.common.generated.resources.unit_group_force
import pascaline.core.common.generated.resources.unit_group_length
import pascaline.core.common.generated.resources.unit_group_mass
import pascaline.core.common.generated.resources.unit_group_power
import pascaline.core.common.generated.resources.unit_group_pressure
import pascaline.core.common.generated.resources.unit_group_speed
import pascaline.core.common.generated.resources.unit_group_temperature
import pascaline.core.common.generated.resources.unit_group_time
import pascaline.core.common.generated.resources.unit_group_volume
import pascaline.core.common.generated.resources.unit_hectare
import pascaline.core.common.generated.resources.unit_hectopascal
import pascaline.core.common.generated.resources.unit_horse_power_mechanical
import pascaline.core.common.generated.resources.unit_hour
import pascaline.core.common.generated.resources.unit_inch
import pascaline.core.common.generated.resources.unit_joule
import pascaline.core.common.generated.resources.unit_kelvin
import pascaline.core.common.generated.resources.unit_kilogram
import pascaline.core.common.generated.resources.unit_kilojoule
import pascaline.core.common.generated.resources.unit_kilometer
import pascaline.core.common.generated.resources.unit_kilometer_per_hour
import pascaline.core.common.generated.resources.unit_kilonewton
import pascaline.core.common.generated.resources.unit_kilopascal
import pascaline.core.common.generated.resources.unit_kilowatt
import pascaline.core.common.generated.resources.unit_kilowatt_hour
import pascaline.core.common.generated.resources.unit_knot
import pascaline.core.common.generated.resources.unit_light_year
import pascaline.core.common.generated.resources.unit_liter
import pascaline.core.common.generated.resources.unit_megapascal
import pascaline.core.common.generated.resources.unit_megawatt
import pascaline.core.common.generated.resources.unit_megawatt_hour
import pascaline.core.common.generated.resources.unit_meter
import pascaline.core.common.generated.resources.unit_meter_per_second
import pascaline.core.common.generated.resources.unit_metric_ton
import pascaline.core.common.generated.resources.unit_microfarad
import pascaline.core.common.generated.resources.unit_microgram
import pascaline.core.common.generated.resources.unit_micrometer
import pascaline.core.common.generated.resources.unit_microsecond
import pascaline.core.common.generated.resources.unit_mile
import pascaline.core.common.generated.resources.unit_mile_per_hour
import pascaline.core.common.generated.resources.unit_millifarad
import pascaline.core.common.generated.resources.unit_milligram
import pascaline.core.common.generated.resources.unit_milliliter
import pascaline.core.common.generated.resources.unit_millimeter
import pascaline.core.common.generated.resources.unit_millimeter_of_mercury
import pascaline.core.common.generated.resources.unit_millinewton
import pascaline.core.common.generated.resources.unit_millisecond
import pascaline.core.common.generated.resources.unit_milliwatt_hour
import pascaline.core.common.generated.resources.unit_minute
import pascaline.core.common.generated.resources.unit_nanofarad
import pascaline.core.common.generated.resources.unit_nanometer
import pascaline.core.common.generated.resources.unit_nanosecond
import pascaline.core.common.generated.resources.unit_newton
import pascaline.core.common.generated.resources.unit_ounce
import pascaline.core.common.generated.resources.unit_parsec
import pascaline.core.common.generated.resources.unit_pascal
import pascaline.core.common.generated.resources.unit_picofarad
import pascaline.core.common.generated.resources.unit_pound
import pascaline.core.common.generated.resources.unit_pound_force
import pascaline.core.common.generated.resources.unit_psi
import pascaline.core.common.generated.resources.unit_radian
import pascaline.core.common.generated.resources.unit_second
import pascaline.core.common.generated.resources.unit_short_ton
import pascaline.core.common.generated.resources.unit_square_centimeter
import pascaline.core.common.generated.resources.unit_square_foot
import pascaline.core.common.generated.resources.unit_square_inch
import pascaline.core.common.generated.resources.unit_square_kilometer
import pascaline.core.common.generated.resources.unit_square_meter
import pascaline.core.common.generated.resources.unit_square_mile
import pascaline.core.common.generated.resources.unit_square_millimeter
import pascaline.core.common.generated.resources.unit_square_yard
import pascaline.core.common.generated.resources.unit_watt
import pascaline.core.common.generated.resources.unit_watt_hour
import pascaline.core.common.generated.resources.unit_week
import pascaline.core.common.generated.resources.unit_yard
import pascaline.core.common.generated.resources.units_menu_amount_of_substance
import pascaline.core.common.generated.resources.units_menu_area_and_volume
import pascaline.core.common.generated.resources.units_menu_astronomy
import pascaline.core.common.generated.resources.units_menu_constants
import pascaline.core.common.generated.resources.units_menu_current
import pascaline.core.common.generated.resources.units_menu_electricity
import pascaline.core.common.generated.resources.units_menu_energy_and_power
import pascaline.core.common.generated.resources.units_menu_food
import pascaline.core.common.generated.resources.units_menu_force_and_pressure
import pascaline.core.common.generated.resources.units_menu_frequency
import pascaline.core.common.generated.resources.units_menu_imperial
import pascaline.core.common.generated.resources.units_menu_length_and_angle
import pascaline.core.common.generated.resources.units_menu_other_units
import pascaline.core.common.generated.resources.units_menu_particles
import pascaline.core.common.generated.resources.units_menu_resistance
import pascaline.core.common.generated.resources.units_menu_time_and_frequency
import pascaline.core.common.generated.resources.units_menu_voltage

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
    "Reduced Planck constant" to Res.string.physical_unit_reduced_planck_constant,
    "Stefan–Boltzmann constant" to Res.string.physical_unit_stefan_boltzmann_constant,
    "Bohr radius" to Res.string.physical_unit_bohr_radius,
    "Atomic mass constant" to Res.string.physical_unit_atomic_mass_constant,
    "Molar volume (0 °C, 1 atm)" to Res.string.physical_unit_molar_volume,
    "Fundamental" to Res.string.units_menu_fundamental,
    "Gravity" to Res.string.units_menu_gravity,
    "Chemistry and heat" to Res.string.units_menu_chemistry_and_heat,
  )
