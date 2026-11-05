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

package com.sadellie.unitto.feature.calculator.place

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.stateIn
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal enum class PlaceTab {
  SUN,
  MOON,
  SKY,
  TIDES,
  TEMPERATURE,
  RAIN,
  ELEVATION,
}

/**
 * Water level of the day every [CURVE_STEP], its high and low waters, spring or neap tides, and how
 * far the sea they are for is.
 */
internal data class TideDay(
  val curve: List<Double>,
  val extremes: List<TideExtreme>,
  val isSpring: Boolean,
  val distanceKm: Double,
)

/** Everything shown for a point on a date, in metric units and UTC milliseconds. */
internal data class PlaceReport(
  val point: GeoPoint,
  val date: LocalDate,
  val zone: String,
  val dayStart: Long,
  val sun: SunDay,
  /** Sun's altitude every [CURVE_STEP] of the day. */
  val sunCurve: List<Double>,
  val moon: MoonState,
  val moonDay: MoonDay,
  val nextFull: Long,
  val nextNew: Long,
  val isNextFullSuper: Boolean,
  val showers: List<ShowerEvent>,
  val season: SeasonEvent,
  val tides: TideDay?,
  val elevation: Double,
  val temperatures: TemperatureClimate,
  /** Millimeters of each month, January first. */
  val precipitation: DoubleArray,
  val climate: Climate,
  /** Best aim of a solar panel on this date, null when the Sun stays down. */
  val dayPanel: PanelAim?,
  /** Best aim of a solar panel that stays in place all year. */
  val yearPanel: PanelAim?,
) {
  /** Average low and high on [date], at the point's elevation. */
  fun dayTemperature(date: LocalDate): Pair<Double, Double> {
    val months = monthsFromMidJanuary(date.dayOfYear, if (daysInMonths(date.year)[1] == 29) 366 else 365)
    return atElevation(temperatureAt(temperatures.lows, months), elevation) to
      atElevation(temperatureAt(temperatures.highs, months), elevation)
  }

  /** Mean temperature of month [index] (0 is January) at the point's elevation. */
  fun monthMean(index: Int): Double =
    atElevation((temperatureAt(temperatures.lows, index.toDouble()) + temperatureAt(temperatures.highs, index.toDouble())) / 2, elevation)

  override fun equals(other: Any?): Boolean = this === other

  override fun hashCode(): Int = dayStart.hashCode() * 31 + point.hashCode()
}

/** Eclipses are searched after the rest, they take longer. */
internal data class EclipseReport(val lunar: Eclipse?, val solar: Eclipse?)

/**
 * A point of the map and a date, the time zone comes with the point. Whenever one changes, the
 * report is worked out again from the bundled data and the astronomy, off the main thread.
 */
internal class PlaceViewModel(userPrefsRepository: UserPreferencesRepository) : ViewModel() {
  val formatterSymbols =
    userPrefsRepository.calculatorPrefs.map { it.formatterSymbols }.stateIn(viewModelScope, null)

  private val data = PlaceData()
  private var reportJob: Job? = null
  private var zones: List<ZoneCity> = emptyList()
  private var started = false

  /** Cities of the map, the biggest first, empty until they are read. */
  var cities by mutableStateOf<List<City>>(emptyList())
    private set

  /** Where the map should fly to, with a number that changes each time. */
  var focus by mutableStateOf<Pair<GeoPoint, Int>?>(null)
    private set

  /** The city closest to the point when it is near, null otherwise. */
  var nearCity by mutableStateOf<City?>(null)
    private set

  var tab by mutableStateOf(PlaceTab.SUN)
  var isMetric by mutableStateOf(placeUsesMetric())
  var point by mutableStateOf(DEFAULT_POINT)
    private set

  var zone by mutableStateOf(deviceZone())
    private set

  var date by mutableStateOf(today(deviceZone()))
    private set

  var report by mutableStateOf<PlaceReport?>(null)
    private set

  var eclipses by mutableStateOf<EclipseReport?>(null)
    private set

  var land by mutableStateOf<LandMap?>(null)
    private set

  /** Why the last report couldn't be made, null when it was. */
  var error by mutableStateOf<String?>(null)
    private set

  init {
    viewModelScope.launch {
      land = runCatching { data.landMap() }.getOrNull()
    }
    viewModelScope.launch {
      zones = runCatching { data.zones() }.getOrDefault(emptyList())
      cities = runCatching { data.cities() }.getOrDefault(emptyList())
      nearCity = nearCity(point)
      recompute()
    }
  }

  /**
   * A new point brings the zone of its closest city, from the cities of the map, then from the zone
   * cities. The date stays.
   */
  fun choosePoint(newPoint: GeoPoint) {
    point = GeoPoint(newPoint.latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE), wrapLongitude(newPoint.longitude))
    nearCity = nearCity(point)
    val cityZone = nearestCity(cities, point)?.first?.zone?.takeIf { it in knownZoneSet }
    (cityZone ?: nearestZone(zones, point))?.let { zone = it }
    recompute()
  }

  /** Chooses [city] and flies the map to it. */
  fun chooseCity(city: City) {
    choosePoint(city.point)
    focus = city.point to ((focus?.second ?: 0) + 1)
  }

  /** The first time the cities are there, flies to the city of [language] on its today. */
  fun startAt(language: String) {
    if (started) return
    val city = defaultCity(cities, language) ?: return
    started = true
    chooseCity(city)
    chooseDate(today())
  }

  private val knownZoneSet by lazy { knownZones().toSet() }

  private fun nearCity(point: GeoPoint): City? =
    nearestCity(cities, point)?.takeIf { it.second <= NEAR_CITY_KM }?.first

  fun chooseDate(newDate: LocalDate) {
    date = newDate
    recompute()
  }

  /** Today where the point is. */
  fun today(): LocalDate = today(zone)

  private fun recompute() {
    reportJob?.cancel()
    val point = point
    val zone = zone
    val date = date
    reportJob =
      viewModelScope.launch {
        try {
          val built = withContext(Dispatchers.Default) { buildReport(point, zone, date) }
          report = built
          error = null
          eclipses = null
          eclipses =
            withContext(Dispatchers.Default) {
              EclipseReport(nextLunarEclipse(built.dayStart, point), nextSolarEclipse(built.dayStart, point))
            }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          // A data file that can't be read leaves the last report
          error = e.message ?: e::class.simpleName
        }
      }
  }

  private suspend fun buildReport(point: GeoPoint, zone: String, date: LocalDate): PlaceReport {
    val dayStart = dayStart(date, zone)
    val noon = dayStart + DAY / 2
    val nextFull = nextPhase(dayStart, FULL)
    val tides = data.tides(point)?.takeIf { it.distanceKm <= MAX_TIDE_DISTANCE_KM }
    val elevation = data.elevation(point)
    val temperatures = data.temperatures(point)
    val precipitation = data.precipitation(point, date.year)
    val partial =
      PlaceReport(
        point = point,
        date = date,
        zone = zone,
        dayStart = dayStart,
        sun = sunDay(dayStart, point),
        sunCurve = curve(dayStart) { sunAltitude(it, point) },
        moon = moonState(noon),
        moonDay = moonDay(dayStart, point),
        nextFull = nextFull,
        nextNew = nextPhase(dayStart, 0.0),
        isNextFullSuper = isSupermoon(nextFull),
        showers = nextShowers(dayStart, point).take(SHOWERS),
        season = nextSeason(dayStart),
        tides =
          tides?.let { site ->
            TideDay(
              curve = curve(dayStart) { t -> tideHeight(site.harmonics, t) },
              extremes = tideExtremes(site.harmonics, dayStart, dayStart + DAY),
              isSpring = isSpringTide(moonState(noon).elongation),
              distanceKm = site.distanceKm,
            )
          },
        elevation = elevation,
        temperatures = temperatures,
        precipitation = precipitation,
        climate = Climate.OCEANIC,
        dayPanel = dayPanelAim(point, dayStart, date.dayOfYear),
        yearPanel = yearPanelAim(point, dayStart(LocalDate(date.year, 1, 1), zone)),
      )
    val means = DoubleArray(MONTHS) { partial.monthMean(it) }
    return partial.copy(climate = koppen(means, precipitation, point.latitude))
  }
}

/** Values of [f] every [CURVE_STEP] of the day from [dayStart], ends included. */
private fun curve(dayStart: Long, f: (Long) -> Double): List<Double> =
  (0..CURVE_POINTS).map { f(dayStart + it * CURVE_STEP) }

/** Midnight of [date] in [zone]. */
internal fun dayStart(date: LocalDate, zone: String): Long {
  val utcMidnight = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
  // The offset at the guess, then again at the moment found, for summer time changes
  var millis = utcMidnight - zoneOffsetMillis(zone, utcMidnight)
  millis = utcMidnight - zoneOffsetMillis(zone, millis)
  return millis
}

internal fun today(zone: String): LocalDate {
  val now = Clock.System.now().toEpochMilliseconds()
  return localDateTime(now, zone).date
}

/** Date and hour at [millis] in [zone]. */
internal fun localDateTime(millis: Long, zone: String): LocalDateTime =
  Instant.fromEpochMilliseconds(millis + zoneOffsetMillis(zone, millis)).toLocalDateTime(TimeZone.UTC)

internal fun LocalDate.nextDay(): LocalDate = plus(1, DateTimeUnit.DAY)

internal const val CURVE_STEP = 10 * MILLIS_PER_MINUTE
internal const val CURVE_POINTS = 144
private const val DAY = 86_400_000L
private const val FULL = 180.0
private const val SHOWERS = 3
private const val MONTHS = 12
private const val MAX_LATITUDE = 89.9
private const val NEAR_CITY_KM = 25.0
private val DEFAULT_POINT = GeoPoint(48.8566, 2.3522)
