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

/**
 * Exponents of SI base units: metre, kilogram, second, ampere, kelvin, mole, candela. Angles are
 * dimensionless (radians).
 */
data class Dimension(
  val m: Int = 0,
  val kg: Int = 0,
  val s: Int = 0,
  val a: Int = 0,
  val k: Int = 0,
  val mol: Int = 0,
  val cd: Int = 0,
) {
  val isDimensionless: Boolean
    get() = this == NONE

  operator fun plus(other: Dimension) =
    Dimension(m + other.m, kg + other.kg, s + other.s, a + other.a, k + other.k, mol + other.mol, cd + other.cd)

  operator fun minus(other: Dimension) = this + other * -1

  operator fun times(power: Int) =
    Dimension(m * power, kg * power, s * power, a * power, k * power, mol * power, cd * power)

  /** Exponents with base unit symbols, in SI order. */
  fun exponents(): List<Pair<String, Int>> =
    listOf("kg" to kg, "m" to m, "s" to s, "A" to a, "K" to k, "mol" to mol, "cd" to cd).filter {
      it.second != 0
    }

  companion object {
    val NONE = Dimension()
  }
}

/**
 * Unit or physical constant, like Epsilon on NumWorks. Written as `_km` in expressions.
 *
 * @property factor Value in SI base units: 1 km = 1000 m.
 * @property offset Only for temperatures in °C and °F: kelvins = value × factor + offset.
 * @property isConstant Physical constant, like speed of light. Results are not shown in them.
 */
data class PhysicalUnit(
  val symbol: String,
  val name: String,
  val factor: String,
  val dimension: Dimension,
  val offset: String? = null,
  val isConstant: Boolean = false,
) {
  /** Text in expressions. */
  val token: String
    get() = Token.Units.PREFIX + symbol

  val isAffine: Boolean
    get() = offset != null
}

/** Units menu, same order as Epsilon toolbox. */
sealed interface UnitMenuItem {
  data class Category(val name: String, val children: List<UnitMenuItem>) : UnitMenuItem

  data class Leaf(val unit: PhysicalUnit) : UnitMenuItem
}

object PhysicalUnits {
  private val LENGTH = Dimension(m = 1)
  private val MASS = Dimension(kg = 1)
  private val TIME = Dimension(s = 1)
  private val CURRENT = Dimension(a = 1)
  private val TEMPERATURE = Dimension(k = 1)
  private val AMOUNT = Dimension(mol = 1)
  private val LUMINOUS = Dimension(cd = 1)
  private val FREQUENCY = Dimension(s = -1)
  private val AREA = Dimension(m = 2)
  private val VOLUME = Dimension(m = 3)
  private val FORCE = Dimension(kg = 1, m = 1, s = -2)
  private val PRESSURE = Dimension(kg = 1, m = -1, s = -2)
  private val ENERGY = Dimension(kg = 1, m = 2, s = -2)
  private val POWER = Dimension(kg = 1, m = 2, s = -3)
  private val CHARGE = Dimension(a = 1, s = 1)
  private val VOLTAGE = Dimension(kg = 1, m = 2, s = -3, a = -1)
  private val CAPACITANCE = Dimension(kg = -1, m = -2, s = 4, a = 2)
  private val RESISTANCE = Dimension(kg = 1, m = 2, s = -3, a = -2)
  private val CONDUCTANCE = Dimension(kg = -1, m = -2, s = 3, a = 2)
  private val INDUCTANCE = Dimension(kg = 1, m = 2, s = -2, a = -2)
  private val MAGNETIC_FIELD = Dimension(kg = 1, s = -2, a = -1)

  private fun unit(symbol: String, name: String, factor: String, dimension: Dimension) =
    PhysicalUnit(symbol, name, factor, dimension)

  private fun constant(symbol: String, name: String, factor: String, dimension: Dimension) =
    PhysicalUnit(symbol, name, factor, dimension, isConstant = true)

  // Length and angle
  private val picometer = unit("pm", "Picometer", "1E-12", LENGTH)
  private val nanometer = unit("nm", "Nanometer", "1E-9", LENGTH)
  private val micrometer = unit("μm", "Micrometer", "1E-6", LENGTH)
  private val millimeter = unit("mm", "Millimeter", "0.001", LENGTH)
  private val centimeter = unit("cm", "Centimeter", "0.01", LENGTH)
  val meter = unit("m", "Meter", "1", LENGTH)
  private val kilometer = unit("km", "Kilometer", "1000", LENGTH)
  private val astronomicalUnit = unit("au", "Astronomical unit", "149597870700", LENGTH)
  private val lightYear = unit("ly", "Light-year", "9460730472580800", LENGTH)
  private val parsec = unit("pc", "Parsec", "30856775814913673", LENGTH)
  private val inch = unit("in", "Inch", "0.0254", LENGTH)
  private val foot = unit("ft", "Foot", "0.3048", LENGTH)
  private val yard = unit("yd", "Yard", "0.9144", LENGTH)
  private val mile = unit("mi", "Mile", "1609.344", LENGTH)
  private val degree =
    unit("°", "Degree", "0.017453292519943295769236907684886127134428718885417", Dimension.NONE)
  private val radian = unit("rad", "Radian", "1", Dimension.NONE)
  private val gradian =
    unit("gon", "Gradian", "0.015707963267948966192313216916397514420985846996876", Dimension.NONE)

  // Time and frequency
  private val nanosecond = unit("ns", "Nanosecond", "1E-9", TIME)
  private val microsecond = unit("μs", "Microsecond", "1E-6", TIME)
  private val millisecond = unit("ms", "Millisecond", "0.001", TIME)
  val second = unit("s", "Second", "1", TIME)
  val minute = unit("min", "Minute", "60", TIME)
  val hour = unit("h", "Hour", "3600", TIME)
  val day = unit("day", "Day", "86400", TIME)
  private val week = unit("week", "Week", "604800", TIME)
  private val month = unit("month", "Month", "2629800", TIME)
  val year = unit("year", "Year", "31557600", TIME)
  private val hertz = unit("Hz", "Hertz", "1", FREQUENCY)
  private val kilohertz = unit("kHz", "Kilohertz", "1000", FREQUENCY)
  private val megahertz = unit("MHz", "Megahertz", "1E6", FREQUENCY)
  private val gigahertz = unit("GHz", "Gigahertz", "1E9", FREQUENCY)

  // Area and volume
  private val hectare = unit("ha", "Hectare", "10000", AREA)
  private val acre = unit("acre", "Acre", "4046.8564224", AREA)
  private val milliliter = unit("mL", "Milliliter", "1E-6", VOLUME)
  private val centiliter = unit("cL", "Centiliter", "1E-5", VOLUME)
  private val deciliter = unit("dL", "Deciliter", "1E-4", VOLUME)
  private val liter = unit("L", "Liter", "0.001", VOLUME)
  private val teaspoon = unit("tsp", "Teaspoon", "0.00000492892159375", VOLUME)
  private val tablespoon = unit("tbsp", "Tablespoon", "0.00001478676478125", VOLUME)
  private val fluidOunce = unit("floz", "Fluid ounce", "0.0000295735295625", VOLUME)
  private val cup = unit("cup", "Cup", "0.0002365882365", VOLUME)
  private val pint = unit("pt", "Pint", "0.000473176473", VOLUME)
  private val quart = unit("qt", "Quart", "0.000946352946", VOLUME)
  private val gallon = unit("gal", "Gallon", "0.003785411784", VOLUME)

  // Mass
  private val microgram = unit("μg", "Microgram", "1E-9", MASS)
  private val milligram = unit("mg", "Milligram", "1E-6", MASS)
  val gram = unit("g", "Gram", "0.001", MASS)
  private val kilogram = unit("kg", "Kilogram", "1", MASS)
  private val tonne = unit("t", "Metric ton", "1000", MASS)
  private val ounce = unit("oz", "Ounce", "0.028349523125", MASS)
  private val pound = unit("lb", "Pound", "0.45359237", MASS)
  private val shortTon = unit("shtn", "Short ton", "907.18474", MASS)
  private val longTon = unit("lgtn", "Long ton", "1016.0469088", MASS)

  // Electricity
  private val microampere = unit("μA", "Microampere", "1E-6", CURRENT)
  private val milliampere = unit("mA", "Milliampere", "0.001", CURRENT)
  val ampere = unit("A", "Ampere", "1", CURRENT)
  private val microvolt = unit("μV", "Microvolt", "1E-6", VOLTAGE)
  private val millivolt = unit("mV", "Millivolt", "0.001", VOLTAGE)
  val volt = unit("V", "Volt", "1", VOLTAGE)
  private val kilovolt = unit("kV", "Kilovolt", "1000", VOLTAGE)
  val ohm = unit("Ω", "Ohm", "1", RESISTANCE)
  private val kiloohm = unit("kΩ", "Kiloohm", "1000", RESISTANCE)
  private val microfarad = unit("μF", "Microfarad", "1E-6", CAPACITANCE)
  private val millifarad = unit("mF", "Millifarad", "0.001", CAPACITANCE)
  val farad = unit("F", "Farad", "1", CAPACITANCE)
  val henry = unit("H", "Henry", "1", INDUCTANCE)
  val coulomb = unit("C", "Coulomb", "1", CHARGE)
  val siemens = unit("S", "Siemens", "1", CONDUCTANCE)
  val tesla = unit("T", "Tesla", "1", MAGNETIC_FIELD)

  // Force and pressure
  private val millinewton = unit("mN", "Millinewton", "0.001", FORCE)
  val newton = unit("N", "Newton", "1", FORCE)
  private val kilonewton = unit("kN", "Kilonewton", "1000", FORCE)
  val pascal = unit("Pa", "Pascal", "1", PRESSURE)
  private val hectopascal = unit("hPa", "Hectopascal", "100", PRESSURE)
  private val bar = unit("bar", "Bar", "100000", PRESSURE)
  private val atmosphere = unit("atm", "Atmosphere", "101325", PRESSURE)

  // Energy and power
  private val millijoule = unit("mJ", "Millijoule", "0.001", ENERGY)
  val joule = unit("J", "Joule", "1", ENERGY)
  private val kilojoule = unit("kJ", "Kilojoule", "1000", ENERGY)
  private val millielectronvolt = unit("meV", "Millielectronvolt", "1.602176634E-22", ENERGY)
  private val electronvolt = unit("eV", "Electronvolt", "1.602176634E-19", ENERGY)
  private val kiloelectronvolt = unit("keV", "Kiloelectronvolt", "1.602176634E-16", ENERGY)
  private val megaelectronvolt = unit("MeV", "Megaelectronvolt", "1.602176634E-13", ENERGY)
  private val microwatt = unit("μW", "Microwatt", "1E-6", POWER)
  private val milliwatt = unit("mW", "Milliwatt", "0.001", POWER)
  val watt = unit("W", "Watt", "1", POWER)
  private val kilowatt = unit("kW", "Kilowatt", "1000", POWER)
  private val megawatt = unit("MW", "Megawatt", "1E6", POWER)
  private val gigawatt = unit("GW", "Gigawatt", "1E9", POWER)
  private val horsepower = unit("hp", "Horsepower", "745.69987158227022", POWER)

  // Temperature
  val kelvin = unit("K", "Kelvin", "1", TEMPERATURE)
  private val celsius = PhysicalUnit("°C", "Celsius", "1", TEMPERATURE, offset = "273.15")
  private val fahrenheit =
    PhysicalUnit(
      "°F",
      "Fahrenheit",
      "0.55555555555555555555555555555555555555555555555556",
      TEMPERATURE,
      // (F + 459.67) × 5/9
      offset = "255.37222222222222222222222222222222222222222222222",
    )

  // Other
  val candela = unit("cd", "Candela", "1", LUMINOUS)
  private val micromole = unit("μmol", "Micromole", "1E-6", AMOUNT)
  private val millimole = unit("mmol", "Millimole", "0.001", AMOUNT)
  val mole = unit("mol", "Mole", "1", AMOUNT)

  // Constants
  private val constants =
    listOf(
      constant("c", "Speed of light", "299792458", Dimension(m = 1, s = -1)),
      constant("e", "Elementary charge", "1.602176634E-19", CHARGE),
      constant("G", "Gravitational constant", "6.67430E-11", Dimension(m = 3, kg = -1, s = -2)),
      constant("g0", "Acceleration of gravity", "9.80665", Dimension(m = 1, s = -2)),
      constant("k", "Boltzmann constant", "1.380649E-23", Dimension(kg = 1, m = 2, s = -2, k = -1)),
      constant("ke", "Coulomb constant", "8987551792.3", Dimension(kg = 1, m = 3, s = -4, a = -2)),
      constant("me", "Electron mass", "9.1093837015E-31", MASS),
      constant("mn", "Neutron mass", "1.67492749804E-27", MASS),
      constant("mp", "Proton mass", "1.67262192369E-27", MASS),
      constant("NA", "Avogadro constant", "6.02214076E23", Dimension(mol = -1)),
      constant("R", "Molar gas constant", "8.314462618", Dimension(kg = 1, m = 2, s = -2, k = -1, mol = -1)),
      constant("ε0", "Vacuum permittivity", "8.8541878128E-12", Dimension(kg = -1, m = -3, s = 4, a = 2)),
      constant("μ0", "Vacuum permeability", "0.00000125663706212", Dimension(kg = 1, m = 1, s = -2, a = -2)),
      constant("hplanck", "Planck constant", "6.62607015E-34", Dimension(kg = 1, m = 2, s = -1)),
    )

  private fun leaves(vararg units: PhysicalUnit) = units.map { UnitMenuItem.Leaf(it) }

  private fun category(name: String, vararg children: UnitMenuItem) =
    UnitMenuItem.Category(name, children.toList())

  /** Same groups and order as Epsilon (metric variant). */
  val menu: List<UnitMenuItem> =
    listOf(
      category(
        "Length and angle",
        category(
          "Length",
          *leaves(picometer, nanometer, micrometer, millimeter, centimeter, meter, kilometer).toTypedArray(),
          *leaves(astronomicalUnit, lightYear, parsec).toTypedArray(),
          category("Imperial", *leaves(inch, foot, yard, mile).toTypedArray()),
        ),
        category("Angle", *leaves(degree, radian, gradian).toTypedArray()),
      ),
      category(
        "Time and frequency",
        category(
          "Time",
          *leaves(nanosecond, microsecond, millisecond, second, minute, hour, day, week, month, year)
            .toTypedArray(),
        ),
        category("Frequency", *leaves(hertz, kilohertz, megahertz, gigahertz).toTypedArray()),
      ),
      category(
        "Area and volume",
        category("Area", *leaves(hectare).toTypedArray(), category("Imperial", *leaves(acre).toTypedArray())),
        category(
          "Volume",
          *leaves(milliliter, centiliter, deciliter, liter).toTypedArray(),
          category(
            "Imperial",
            *leaves(teaspoon, tablespoon, fluidOunce, cup, pint, quart, gallon).toTypedArray(),
          ),
        ),
      ),
      category(
        "Mass",
        *leaves(microgram, milligram, gram, kilogram, tonne).toTypedArray(),
        category("Imperial", *leaves(ounce, pound, shortTon, longTon).toTypedArray()),
      ),
      category(
        "Electricity",
        category("Current", *leaves(microampere, milliampere, ampere).toTypedArray()),
        category("Voltage", *leaves(microvolt, millivolt, volt, kilovolt).toTypedArray()),
        category("Resistance", *leaves(ohm, kiloohm).toTypedArray()),
        category("Capacitance", *leaves(microfarad, millifarad, farad).toTypedArray()),
        category("Other units", *leaves(henry, coulomb, siemens, tesla).toTypedArray()),
      ),
      category(
        "Force and pressure",
        category("Force", *leaves(millinewton, newton, kilonewton).toTypedArray()),
        category("Pressure", *leaves(pascal, hectopascal, bar, atmosphere).toTypedArray()),
      ),
      category(
        "Energy and power",
        category(
          "Energy",
          category("Joule", *leaves(millijoule, joule, kilojoule).toTypedArray()),
          category(
            "Electronvolt",
            *leaves(millielectronvolt, electronvolt, kiloelectronvolt, megaelectronvolt).toTypedArray(),
          ),
        ),
        category(
          "Power",
          *leaves(microwatt, milliwatt, watt, kilowatt, megawatt, gigawatt, horsepower).toTypedArray(),
        ),
      ),
      category("Temperature", *leaves(kelvin, celsius, fahrenheit).toTypedArray()),
      category(
        "Other units",
        *leaves(candela).toTypedArray(),
        category("Amount of substance", *leaves(micromole, millimole, mole).toTypedArray()),
      ),
      category("Constants", *constants.map { UnitMenuItem.Leaf(it) }.toTypedArray()),
    )

  /** Every unit and constant, longest symbol first (for matching `_mmol` before `_m`). */
  val all: List<PhysicalUnit> by lazy {
    fun collect(items: List<UnitMenuItem>): List<PhysicalUnit> =
      items.flatMap {
        when (it) {
          is UnitMenuItem.Category -> collect(it.children)
          is UnitMenuItem.Leaf -> listOf(it.unit)
        }
      }
    (collect(menu) + prefixed).distinctBy { it.symbol }.sortedByDescending { it.symbol.length }
  }

  /**
   * Named SI units with prefixes from femto to peta that are not in the menu (THz, PHz, nN, …).
   * Results are shown in them: 55E15 Hz ==> 55 PHz. Mass stops at kg, length at km and time at s:
   * Mm and Gs are hard to read.
   */
  private val prefixed: List<PhysicalUnit> by lazy {
    derived.flatMap { base ->
      val baseExponent = if (base == gram) GRAM_EXPONENT else 0
      SI_PREFIXES.filter { (_, _, exponent) ->
        when (base) {
          gram -> exponent <= KILO_EXPONENT
          meter -> exponent <= KILO_EXPONENT
          second -> exponent < 0
          else -> true
        }
      }
        .map { (symbol, name, exponent) ->
          PhysicalUnit(
            symbol = symbol + base.symbol,
            name = name + base.name.lowercase(),
            factor = "1E${exponent + baseExponent}",
            dimension = base.dimension,
          )
        }
    }
  }

  /** Unit tokens (`_km`), longest first. */
  val tokens: List<String> by lazy { all.map { it.token } }

  /** Unit token at [index] of [text], null if there is none. */
  fun tokenAt(text: CharSequence, index: Int): PhysicalUnit? {
    if (text.getOrNull(index) != Token.Units.PREFIX.first()) return null
    return all.firstOrNull { text.startsWith(it.token, index) }
  }

  fun bySymbol(symbol: String): PhysicalUnit? = all.firstOrNull { it.symbol == symbol }

  /**
   * Units to show results in: 2 kg·m·s⁻² is shown as 2 N. Others of the same family (kN, mN) are
   * used to pick a prefix. Time is shown in the biggest unit that gives a value of at least 1.
   */
  val derived: List<PhysicalUnit> by lazy {
    listOf(
      meter,
      gram,
      second,
      ampere,
      kelvin,
      mole,
      candela,
      newton,
      joule,
      watt,
      pascal,
      coulomb,
      volt,
      farad,
      ohm,
      siemens,
      henry,
      tesla,
      hertz,
    )
  }

  val timeUnits: List<PhysicalUnit> by lazy { listOf(year, day, hour, minute, second) }

  /** Symbol, name and power of ten. Centi, deci and hecto are only used where Epsilon has them. */
  private val SI_PREFIXES =
    listOf(
      Triple("P", "Peta", 15),
      Triple("T", "Tera", 12),
      Triple("G", "Giga", 9),
      Triple("M", "Mega", 6),
      Triple("k", "Kilo", 3),
      Triple("m", "Milli", -3),
      Triple("μ", "Micro", -6),
      Triple("n", "Nano", -9),
      Triple("p", "Pico", -12),
      Triple("f", "Femto", -15),
    )
  private const val GRAM_EXPONENT = -3
  private const val KILO_EXPONENT = 3
}
