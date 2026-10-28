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

package com.sadellie.unitto.feature.calculator.elements

/** Families of the periodic table, as colored in the NumWorks Elements app. */
internal enum class ElementFamily {
  ALKALI_METAL,
  ALKALINE_EARTH_METAL,
  TRANSITION_METAL,
  POST_TRANSITION_METAL,
  LANTHANIDE,
  ACTINIDE,
  METALLOID,
  NONMETAL,
  HALOGEN,
  NOBLE_GAS,
  UNKNOWN,
}

/**
 * Chemical element.
 *
 * @property mass Standard atomic weight in g/mol. For elements without stable isotopes, the mass
 *   number of the most stable one, see [isMassNumber].
 * @property electronegativity Pauling scale, null when it is not known.
 */
internal class Element(
  val number: Int,
  val symbol: String,
  val name: String,
  val mass: Double,
  val isMassNumber: Boolean,
  val electronegativity: Double?,
) {
  val family: ElementFamily
    get() = familyOf(number)

  /** 1 to 7. */
  val period: Int
    get() = PERIOD_ENDS.indexOfFirst { number <= it } + 1

  /** 1 to 18, null for lanthanides and actinides (except La and Ac, which are in group 3). */
  val group: Int?
    get() {
      val start = PERIOD_ENDS.getOrElse(period - 2) { 0 }
      val index = number - start
      return when (period) {
        1 -> if (number == 1) 1 else 18
        2, 3 -> if (index <= 2) index else index + 10
        4, 5 -> index
        else ->
          when {
            index <= 2 -> index
            index == 3 -> 3
            index <= F_BLOCK_END -> null
            else -> index - F_BLOCK_SIZE
          }
      }
    }

  /** Electron configuration with the noble gas core: `[Ar] 3d⁶ 4s²`. */
  val configuration: String
    get() = electronConfiguration(number)
}

/** Every element by atomic number, from hydrogen to oganesson. */
internal val ELEMENTS: List<Element> by lazy {
  ELEMENT_DATA.trimIndent().lines().mapIndexed { index, line ->
    val (symbol, name, mass, electronegativity) = line.split(' ')
    Element(
      number = index + 1,
      symbol = symbol,
      name = name,
      mass = mass.trim('[', ']').toDouble(),
      isMassNumber = mass.startsWith('['),
      electronegativity = electronegativity.toDoubleOrNull(),
    )
  }
}

/**
 * Row and column in the drawn table, both from 0. Lanthanides and actinides go in two extra rows
 * under the table (rows 8 and 9, after an empty row), like in most periodic tables.
 */
internal fun Element.tablePosition(): Pair<Int, Int> {
  val group = group
  if (group != null) return (period - 1) to (group - 1)
  val first = if (period == 6) LANTHANUM else ACTINIUM
  // Ce to Lu (Th to Lr) after La (Ac), from the fourth column
  return (period + 2) to (number - first + 3)
}

private fun familyOf(number: Int): ElementFamily =
  when (number) {
    3, 11, 19, 37, 55, 87 -> ElementFamily.ALKALI_METAL
    4, 12, 20, 38, 56, 88 -> ElementFamily.ALKALINE_EARTH_METAL
    in 57..71 -> ElementFamily.LANTHANIDE
    in 89..103 -> ElementFamily.ACTINIDE
    5, 14, 32, 33, 51, 52 -> ElementFamily.METALLOID
    1, 6, 7, 8, 15, 16, 34 -> ElementFamily.NONMETAL
    9, 17, 35, 53, 85 -> ElementFamily.HALOGEN
    2, 10, 18, 36, 54, 86 -> ElementFamily.NOBLE_GAS
    13, 31, 49, 50, 81, 82, 83, 84 -> ElementFamily.POST_TRANSITION_METAL
    in 21..30, in 39..48, in 72..80, in 104..108 -> ElementFamily.TRANSITION_METAL
    else -> ElementFamily.UNKNOWN
  }

/** Subshells in the order they are filled (Madelung rule): 1s, 2s, 2p, 3s, 3p, 4s, 3d… */
private val FILLING_ORDER: List<Pair<Int, Int>> =
  (1..8)
    .flatMap { sum -> (0..3).mapNotNull { l -> (sum - l).takeIf { n -> n > l }?.let { n -> n to l } } }
    .sortedWith(compareBy({ it.first + it.second }, { it.first }))

/**
 * Configuration by the Madelung rule, except for elements that are known to differ (Cr, Cu…).
 * Subshells are written by n, then by l: `[Kr] 4d¹⁰ 5s² 5p⁶`.
 */
internal fun electronConfiguration(number: Int): String {
  CONFIGURATION_EXCEPTIONS[number]?.let { return it }
  val core = NOBLE_GASES.lastOrNull { it < number }
  val filled = fill(number)
  val coreFilled = core?.let { fill(it) } ?: emptyMap()
  val outer =
    filled.entries
      .filter { (subshell, electrons) -> electrons != (coreFilled[subshell] ?: 0) }
      .sortedWith(compareBy({ it.key.first }, { it.key.second }))
      .joinToString(" ") { (subshell, electrons) -> subshell.label() + electrons.superscript() }
  val prefix = core?.let { "[${ELEMENTS[it - 1].symbol}]" }
  return listOfNotNull(prefix, outer.ifEmpty { null }).joinToString(" ")
}

private fun fill(electrons: Int): Map<Pair<Int, Int>, Int> {
  var left = electrons
  val subshells = linkedMapOf<Pair<Int, Int>, Int>()
  for (subshell in FILLING_ORDER) {
    if (left == 0) break
    val count = minOf(left, 2 * (2 * subshell.second + 1))
    subshells[subshell] = count
    left -= count
  }
  return subshells
}

private fun Pair<Int, Int>.label() = "$first${"spdf"[second]}"

private fun Int.superscript(): String = toString().map { SUPERSCRIPTS[it - '0'] }.joinToString("")

private const val SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹"
private val NOBLE_GASES = listOf(2, 10, 18, 36, 54, 86)
private val PERIOD_ENDS = listOf(2, 10, 18, 36, 54, 86, 118)
private const val LANTHANUM = 57
private const val ACTINIUM = 89
/** In periods 6 and 7: positions 4 to 17 are Ce to Lu (Th to Lr). */
private const val F_BLOCK_END = 17
private const val F_BLOCK_SIZE = 14

/** Ground states that don't follow the Madelung rule. */
private val CONFIGURATION_EXCEPTIONS =
  mapOf(
    24 to "[Ar] 3d⁵ 4s¹",
    29 to "[Ar] 3d¹⁰ 4s¹",
    41 to "[Kr] 4d⁴ 5s¹",
    42 to "[Kr] 4d⁵ 5s¹",
    44 to "[Kr] 4d⁷ 5s¹",
    45 to "[Kr] 4d⁸ 5s¹",
    46 to "[Kr] 4d¹⁰",
    47 to "[Kr] 4d¹⁰ 5s¹",
    57 to "[Xe] 5d¹ 6s²",
    58 to "[Xe] 4f¹ 5d¹ 6s²",
    64 to "[Xe] 4f⁷ 5d¹ 6s²",
    78 to "[Xe] 4f¹⁴ 5d⁹ 6s¹",
    79 to "[Xe] 4f¹⁴ 5d¹⁰ 6s¹",
    89 to "[Rn] 6d¹ 7s²",
    90 to "[Rn] 6d² 7s²",
    91 to "[Rn] 5f² 6d¹ 7s²",
    92 to "[Rn] 5f³ 6d¹ 7s²",
    93 to "[Rn] 5f⁴ 6d¹ 7s²",
    96 to "[Rn] 5f⁷ 6d¹ 7s²",
    103 to "[Rn] 5f¹⁴ 7s² 7p¹",
  )

/**
 * Symbol, name, atomic weight (IUPAC, mass number of the most stable isotope in brackets) and
 * Pauling electronegativity (`-` when unknown), one element per line.
 */
private const val ELEMENT_DATA =
  """
  H Hydrogen 1.008 2.20
  He Helium 4.0026 -
  Li Lithium 6.94 0.98
  Be Beryllium 9.0122 1.57
  B Boron 10.81 2.04
  C Carbon 12.011 2.55
  N Nitrogen 14.007 3.04
  O Oxygen 15.999 3.44
  F Fluorine 18.998 3.98
  Ne Neon 20.180 -
  Na Sodium 22.990 0.93
  Mg Magnesium 24.305 1.31
  Al Aluminium 26.982 1.61
  Si Silicon 28.085 1.90
  P Phosphorus 30.974 2.19
  S Sulfur 32.06 2.58
  Cl Chlorine 35.45 3.16
  Ar Argon 39.95 -
  K Potassium 39.098 0.82
  Ca Calcium 40.078 1.00
  Sc Scandium 44.956 1.36
  Ti Titanium 47.867 1.54
  V Vanadium 50.942 1.63
  Cr Chromium 51.996 1.66
  Mn Manganese 54.938 1.55
  Fe Iron 55.845 1.83
  Co Cobalt 58.933 1.88
  Ni Nickel 58.693 1.91
  Cu Copper 63.546 1.90
  Zn Zinc 65.38 1.65
  Ga Gallium 69.723 1.81
  Ge Germanium 72.630 2.01
  As Arsenic 74.922 2.18
  Se Selenium 78.971 2.55
  Br Bromine 79.904 2.96
  Kr Krypton 83.798 3.00
  Rb Rubidium 85.468 0.82
  Sr Strontium 87.62 0.95
  Y Yttrium 88.906 1.22
  Zr Zirconium 91.224 1.33
  Nb Niobium 92.906 1.6
  Mo Molybdenum 95.95 2.16
  Tc Technetium [98] 1.9
  Ru Ruthenium 101.07 2.2
  Rh Rhodium 102.91 2.28
  Pd Palladium 106.42 2.20
  Ag Silver 107.87 1.93
  Cd Cadmium 112.41 1.69
  In Indium 114.82 1.78
  Sn Tin 118.71 1.96
  Sb Antimony 121.76 2.05
  Te Tellurium 127.60 2.1
  I Iodine 126.90 2.66
  Xe Xenon 131.29 2.6
  Cs Caesium 132.91 0.79
  Ba Barium 137.33 0.89
  La Lanthanum 138.91 1.10
  Ce Cerium 140.12 1.12
  Pr Praseodymium 140.91 1.13
  Nd Neodymium 144.24 1.14
  Pm Promethium [145] 1.13
  Sm Samarium 150.36 1.17
  Eu Europium 151.96 1.2
  Gd Gadolinium 157.25 1.20
  Tb Terbium 158.93 1.1
  Dy Dysprosium 162.50 1.22
  Ho Holmium 164.93 1.23
  Er Erbium 167.26 1.24
  Tm Thulium 168.93 1.25
  Yb Ytterbium 173.05 1.1
  Lu Lutetium 174.97 1.27
  Hf Hafnium 178.49 1.3
  Ta Tantalum 180.95 1.5
  W Tungsten 183.84 2.36
  Re Rhenium 186.21 1.9
  Os Osmium 190.23 2.2
  Ir Iridium 192.22 2.20
  Pt Platinum 195.08 2.28
  Au Gold 196.97 2.54
  Hg Mercury 200.59 2.00
  Tl Thallium 204.38 1.62
  Pb Lead 207.2 2.33
  Bi Bismuth 208.98 2.02
  Po Polonium [209] 2.0
  At Astatine [210] 2.2
  Rn Radon [222] 2.2
  Fr Francium [223] 0.7
  Ra Radium [226] 0.9
  Ac Actinium [227] 1.1
  Th Thorium 232.04 1.3
  Pa Protactinium 231.04 1.5
  U Uranium 238.03 1.38
  Np Neptunium [237] 1.36
  Pu Plutonium [244] 1.28
  Am Americium [243] 1.3
  Cm Curium [247] 1.3
  Bk Berkelium [247] 1.3
  Cf Californium [251] 1.3
  Es Einsteinium [252] 1.3
  Fm Fermium [257] 1.3
  Md Mendelevium [258] 1.3
  No Nobelium [259] 1.3
  Lr Lawrencium [266] -
  Rf Rutherfordium [267] -
  Db Dubnium [268] -
  Sg Seaborgium [269] -
  Bh Bohrium [270] -
  Hs Hassium [269] -
  Mt Meitnerium [278] -
  Ds Darmstadtium [281] -
  Rg Roentgenium [282] -
  Cn Copernicium [285] -
  Nh Nihonium [286] -
  Fl Flerovium [289] -
  Mc Moscovium [290] -
  Lv Livermorium [293] -
  Ts Tennessine [294] -
  Og Oganesson [294] -
  """
