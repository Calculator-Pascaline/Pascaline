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

package io.github.calculatorpascaline.feature.calculator.inflation

import kotlin.math.pow
import org.jetbrains.compose.resources.StringResource
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.changeover_baguette
import pascaline.core.common.generated.resources.changeover_loaf
import pascaline.core.common.generated.resources.country_austria
import pascaline.core.common.generated.resources.country_belgium
import pascaline.core.common.generated.resources.country_finland
import pascaline.core.common.generated.resources.country_france
import pascaline.core.common.generated.resources.country_germany
import pascaline.core.common.generated.resources.country_greece
import pascaline.core.common.generated.resources.country_ireland
import pascaline.core.common.generated.resources.country_italy
import pascaline.core.common.generated.resources.country_netherlands
import pascaline.core.common.generated.resources.country_portugal
import pascaline.core.common.generated.resources.country_spain
import pascaline.core.common.generated.resources.country_united_kingdom
import pascaline.core.common.generated.resources.currency_australian_dollar
import pascaline.core.common.generated.resources.currency_belgian_franc
import pascaline.core.common.generated.resources.currency_canadian_dollar
import pascaline.core.common.generated.resources.currency_deutsche_mark
import pascaline.core.common.generated.resources.currency_drachma
import pascaline.core.common.generated.resources.currency_escudo
import pascaline.core.common.generated.resources.currency_euro
import pascaline.core.common.generated.resources.currency_french_franc
import pascaline.core.common.generated.resources.currency_guilder
import pascaline.core.common.generated.resources.currency_irish_pound
import pascaline.core.common.generated.resources.currency_lira
import pascaline.core.common.generated.resources.currency_lsd
import pascaline.core.common.generated.resources.currency_markka
import pascaline.core.common.generated.resources.currency_old_franc
import pascaline.core.common.generated.resources.currency_old_markka
import pascaline.core.common.generated.resources.currency_peseta
import pascaline.core.common.generated.resources.currency_pound
import pascaline.core.common.generated.resources.currency_schilling
import pascaline.core.common.generated.resources.currency_swiss_franc
import pascaline.core.common.generated.resources.currency_us_dollar
import pascaline.core.common.generated.resources.currency_yen
import pascaline.core.common.generated.resources.currency_yuan

/**
 * Consumer prices of a country year by year.
 *
 * @property baseYear First year, prices are 1 in it.
 * @property rates Inflation in % of each year after [baseYear], compared with the year before.
 */
internal class PriceHistory(val baseYear: Int, val rates: DoubleArray) {
  val lastYear: Int
    get() = baseYear + rates.size

  val years: IntRange
    get() = baseYear..lastYear

  /** Prices of each year, 1 in [baseYear]. */
  private val levels: DoubleArray by lazy {
    val levels = DoubleArray(rates.size + 1)
    levels[0] = 1.0
    rates.forEachIndexed { index, rate -> levels[index + 1] = levels[index] * (1 + rate / PERCENT) }
    levels
  }

  /** Prices in [year] compared with [baseYear]. */
  fun level(year: Int): Double = levels[(year - baseYear).coerceIn(0, rates.size)]

  /** Inflation of [year] in %, null for [baseYear] that has nothing before it. */
  fun rate(year: Int): Double? = rates.getOrNull(year - baseYear - 1)

  /** What [amount] of [from] buys in [to]: prices multiplied by how much they went up. */
  fun value(amount: Double, from: Int, to: Int): Double = amount * level(to) / level(from)

  /** Average inflation per year from [from] to [to], in %. */
  fun yearlyRate(from: Int, to: Int): Double {
    if (from == to) return 0.0
    val ratio = level(to) / level(from)
    return (ratio.pow(1.0 / (to - from)) - 1) * PERCENT
  }
}

/** A currency of today, with the inflation of where it is used. */
internal class Currency(val name: StringResource, val symbol: String, val history: PriceHistory)

/** Currencies of the inflation tool, the euro with the inflation of the euro area. */
internal val CURRENCIES: List<Currency> by lazy {
  listOf(
    Currency(Res.string.currency_euro, "€", PriceData.EURO_AREA),
    Currency(Res.string.currency_us_dollar, "$", PriceData.UNITED_STATES),
    Currency(Res.string.currency_pound, "£", PriceData.UNITED_KINGDOM),
    Currency(Res.string.currency_yen, "¥", PriceData.JAPAN),
    Currency(Res.string.currency_swiss_franc, "CHF", PriceData.SWITZERLAND),
    Currency(Res.string.currency_canadian_dollar, "C$", PriceData.CANADA),
    Currency(Res.string.currency_australian_dollar, "A$", PriceData.AUSTRALIA),
    Currency(Res.string.currency_yuan, "CN¥", PriceData.CHINA),
  )
}

/**
 * One currency in the history of a country.
 *
 * @property until Last year it was used, null for the one used today.
 * @property perNext How many of it make one of the next currency: 100 old francs are 1 franc.
 * @property fixedIn Year the rate to the next currency was fixed, when it is before [until] + 1:
 *   the euro was fixed in 1999, its coins and notes came in 2002.
 */
internal class Era(
  val name: StringResource,
  val symbol: String,
  val since: Int,
  val until: Int?,
  val perNext: Double = 1.0,
  val fixedIn: Int? = null,
)

/**
 * The currencies of a country one after the other, and its prices.
 *
 * @property pay Average yearly pay of an employee, in the currency used today.
 * @property bread A loaf and its price year by year, when there is one.
 * @property living Food prices, take-home pay and hours of work, when there are.
 */
internal class Country(
  val name: StringResource,
  val history: PriceHistory,
  val eras: List<Era>,
  val pay: YearlyValues? = null,
  val bread: Bread? = null,
  val living: Living? = null,
) {
  /** Years an amount in [era] can be from, where there are prices. */
  fun years(era: Era): IntRange = maxOf(era.since, history.baseYear)..minOf(era.until ?: history.lastYear, history.lastYear)
}

private fun euro(since: Int) = Era(Res.string.currency_euro, "€", since, null)

private fun toEuro(name: StringResource, symbol: String, since: Int, rate: Double, fixedIn: Int = EURO_FIXED) =
  Era(name, symbol, since, until = EURO_CASH - 1, perNext = rate, fixedIn = fixedIn)

/** Countries that changed their currency. Euro rates are the ones fixed for good in 1999 and 2001. */
internal val COUNTRIES: List<Country> by lazy {
  listOf(
    Country(
      Res.string.country_france,
      PriceData.FRANCE,
      pay = PayData.FRANCE,
      living = LivingData.FRANCE,
      bread = Bread(Res.string.changeover_baguette, BreadData.BAGUETTE_KG, perLoaf = BAGUETTE_KG),
      eras = listOf(
        // 100 old francs became 1 new franc on 1 January 1960
        Era(Res.string.currency_old_franc, "AF", FIRST_YEAR, until = 1959, perNext = 100.0),
        toEuro(Res.string.currency_french_franc, "F", 1960, 6.55957),
        euro(EURO_FIXED),
      ),
    ),
    Country(
      Res.string.country_germany,
      PriceData.GERMANY,
      pay = PayData.GERMANY,
      living = LivingData.GERMANY,
      eras = listOf(toEuro(Res.string.currency_deutsche_mark, "DM", FIRST_YEAR, 1.95583), euro(EURO_FIXED)),
    ),
    Country(
      Res.string.country_italy,
      PriceData.ITALY,
      pay = PayData.ITALY,
      living = LivingData.ITALY,
      eras = listOf(toEuro(Res.string.currency_lira, "L", FIRST_YEAR, 1936.27), euro(EURO_FIXED)),
    ),
    Country(
      Res.string.country_spain,
      PriceData.SPAIN,
      pay = PayData.SPAIN,
      living = LivingData.SPAIN,
      eras = listOf(toEuro(Res.string.currency_peseta, "Pta", FIRST_YEAR, 166.386), euro(EURO_FIXED)),
    ),
    Country(
      Res.string.country_netherlands,
      PriceData.NETHERLANDS,
      pay = PayData.NETHERLANDS,
      living = LivingData.NETHERLANDS,
      eras = listOf(toEuro(Res.string.currency_guilder, "ƒ", FIRST_YEAR, 2.20371), euro(EURO_FIXED)),
    ),
    Country(
      Res.string.country_belgium,
      PriceData.BELGIUM,
      pay = PayData.BELGIUM,
      living = LivingData.BELGIUM,
      eras = listOf(toEuro(Res.string.currency_belgian_franc, "FB", FIRST_YEAR, 40.3399), euro(EURO_FIXED)),
    ),
    Country(
      Res.string.country_austria,
      PriceData.AUSTRIA,
      pay = PayData.AUSTRIA,
      living = LivingData.AUSTRIA,
      eras = listOf(toEuro(Res.string.currency_schilling, "S", FIRST_YEAR, 13.7603), euro(EURO_FIXED)),
    ),
    Country(
      Res.string.country_portugal,
      PriceData.PORTUGAL,
      pay = PayData.PORTUGAL,
      living = LivingData.PORTUGAL,
      eras = listOf(toEuro(Res.string.currency_escudo, "Esc", FIRST_YEAR, 200.482), euro(EURO_FIXED)),
    ),
    Country(
      Res.string.country_ireland,
      PriceData.IRELAND,
      pay = PayData.IRELAND,
      living = LivingData.IRELAND,
      eras = listOf(toEuro(Res.string.currency_irish_pound, "IR£", FIRST_YEAR, 0.787564), euro(EURO_FIXED)),
    ),
    Country(
      Res.string.country_finland,
      PriceData.FINLAND,
      pay = PayData.FINLAND,
      living = LivingData.FINLAND,
      eras = listOf(
        // 100 old markkaa became 1 new markka on 1 January 1963
        Era(Res.string.currency_old_markka, "mk", FIRST_YEAR, until = 1962, perNext = 100.0),
        toEuro(Res.string.currency_markka, "mk", 1963, 5.94573),
        euro(EURO_FIXED),
      ),
    ),
    Country(
      Res.string.country_greece,
      PriceData.GREECE,
      pay = PayData.GREECE,
      living = LivingData.GREECE,
      // Greece joined in 2001, with euro coins and notes in 2002 like the others
      eras = listOf(toEuro(Res.string.currency_drachma, "Dr", FIRST_YEAR, 340.75, fixedIn = 2001), euro(2001)),
    ),
    Country(
      Res.string.country_united_kingdom,
      PriceData.UNITED_KINGDOM,
      pay = PayData.UNITED_KINGDOM,
      living = LivingData.UNITED_KINGDOM,
      bread = Bread(Res.string.changeover_loaf, BreadData.WHITE_LOAF_PENCE, perLoaf = 1.0 / PENCE_PER_POUND),
      eras = listOf(
        // Decimal Day, 15 February 1971: the pound stayed, it was split in 100 pence instead of 240
        Era(Res.string.currency_lsd, "£sd", FIRST_YEAR, until = 1970),
        Era(Res.string.currency_pound, "£", 1971, null),
      ),
    ),
  )
}

/** Values year by year from [firstYear], NaN for a year without one. */
internal class YearlyValues(val firstYear: Int, private val values: DoubleArray) {
  val lastYear: Int
    get() = firstYear + values.size - 1

  /** The value of [year], null when there is none. */
  operator fun get(year: Int): Double? = values.getOrNull(year - firstYear)?.takeUnless { it.isNaN() }

  /** The last value up to [year] and its year, null when there is none. */
  fun latest(year: Int): Pair<Int, Double>? =
    (minOf(year, lastYear) downTo firstYear).firstNotNullOfOrNull { y -> get(y)?.let { y to it } }
}

/**
 * A loaf of bread of a country.
 *
 * @property prices Prices year by year, in the currency used today.
 * @property perLoaf How much of a price is one loaf: a quarter for a 250 g baguette priced by the kilo.
 */
internal class Bread(val name: StringResource, val prices: YearlyValues, val perLoaf: Double = 1.0) {
  fun price(year: Int): Double? = prices[year]?.times(perLoaf)
}

/**
 * Food prices, take-home pay and hours of work of a country.
 *
 * @property food Price index of food and non-alcoholic drinks, year by year.
 * @property prices Price index of everything, from the same source as [food].
 * @property netPay Yearly take-home pay of a single worker earning the average wage, in the currency used
 *   today, for the years Eurostat has.
 * @property hours Hours worked in a year by a worker.
 */
internal class Living(val food: YearlyValues, val prices: YearlyValues, val netPay: YearlyValues, val hours: YearlyValues)

/**
 * Yearly take-home pay in [year], in the currency used today. Where Eurostat has none, the average pay
 * of the year times the share of it that was taken home in the closest year with both.
 */
internal fun Country.netPay(year: Int): Double? {
  val living = living ?: return pay?.get(year)
  val gross = pay?.get(year)
  return living.netPay[year] ?: gross?.let { it * (takenHome(living, year) ?: return null) }
}

/** The share of the average pay taken home in the year closest to [year] with both. */
private fun Country.takenHome(living: Living, year: Int): Double? =
  (living.netPay.firstYear..living.netPay.lastYear)
    .mapNotNull { y ->
      val net = living.netPay[y]
      val gross = pay?.get(y)
      if (net != null && gross != null) y to net / gross else null
    }
    .minByOrNull { (y, _) -> kotlin.math.abs(y - year) }
    ?.second

/** Take-home pay for an hour of work in [year], with the hours worked that year or the closest before. */
internal fun Country.hourlyPay(year: Int): Double? {
  val yearly = netPay(year) ?: return null
  val hours = living?.hours?.let { it[year] ?: it.latest(year)?.second } ?: return null
  return yearly / hours
}

/**
 * One year compared with today: a loaf, the take-home pay of a month and of an hour, null where there is
 * no data.
 */
internal data class Side(val year: Int, val amount: Double, val loaf: Double?, val monthlyPay: Double?, val hourlyPay: Double?) {
  /** How many loaves the amount buys. */
  val loaves: Double?
    get() = loaf?.let { amount / it }

  /** The amount as a share of a month of take-home pay, in %. */
  val payShare: Double?
    get() = monthlyPay?.let { amount / it * PERCENT }

  /** Minutes of work that buy a loaf. */
  val loafMinutes: Double?
    get() = if (loaf != null && hourlyPay != null) loaf / hourlyPay * MINUTES_PER_HOUR else null

  /** Hours of work that earn the amount. */
  val workHours: Double?
    get() = hourlyPay?.let { amount / it }
}

/**
 * [amount], in the currency used today, in [year] and its value today with the inflation, each with
 * the bread of its time and the take-home pay. Null when the country has neither for [year]; today
 * only has what [year] has, to compare.
 */
internal fun Country.thenAndNow(amount: Double, year: Int): Pair<Side, Side>? {
  val today = history.lastYear
  val loafThen = bread?.price(year)
  val payThen = netPay(year)
  if (loafThen == null && payThen == null) return null
  val then = Side(year, amount, loafThen, payThen?.div(MONTHS), hourlyPay(year))
  val payYear = (today downTo year).firstOrNull { netPay(it) != null } ?: today
  val now =
    Side(
      today,
      history.value(amount, year, today),
      loaf = bread?.prices?.latest(today)?.second?.times(bread.perLoaf)?.takeIf { loafThen != null },
      monthlyPay = netPay(payYear)?.div(MONTHS)?.takeIf { payThen != null },
      hourlyPay = hourlyPay(payYear)?.takeIf { then.hourlyPay != null },
    )
  return then to now
}

/**
 * How much food and all prices rose from [from] to [to], as shares: 0.84 is +84 %.
 */
internal class FoodRise(val from: Int, val to: Int, val food: Double, val prices: Double)

/** Food against all prices from [year] to the last year with both, null without an index for [year]. */
internal fun Country.foodRise(year: Int): FoodRise? {
  val living = living ?: return null
  val last = living.food.latest(history.lastYear)?.first?.takeIf { it > year }
  val food = last?.let { rise(living.food, year, it) }
  val prices = last?.let { rise(living.prices, year, it) }
  return if (last == null || food == null || prices == null) null else FoodRise(year, last, food, prices)
}

/** How much [values] rose from [from] to [to], as a share, null when one of them is missing. */
private fun rise(values: YearlyValues, from: Int, to: Int): Double? {
  val start = values[from] ?: return null
  return values[to]?.let { it / start - 1 }
}

/** How many of the currency used today one of [era] is worth: 6.55957 francs for a euro gives 1 / 6.55957. */
internal fun Country.toToday(era: Era): Double =
  eras.drop(eras.indexOf(era)).dropLast(1).fold(1.0) { factor, next -> factor / next.perNext }

/** Shillings in a pound and pence in a shilling before 1971. */
internal const val SHILLINGS_PER_POUND = 20
internal const val PENCE_PER_SHILLING = 12

private const val PERCENT = 100
private const val MINUTES_PER_HOUR = 60
private const val MONTHS = 12
private const val PENCE_PER_POUND = 100
private const val BAGUETTE_KG = 0.25
private const val FIRST_YEAR = 1900
private const val EURO_FIXED = 1999
private const val EURO_CASH = 2002
