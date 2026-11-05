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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.place_astronomical_twilight
import unitto.core.common.generated.resources.place_at_sea
import unitto.core.common.generated.resources.place_blue_hour
import unitto.core.common.generated.resources.place_civil_twilight
import unitto.core.common.generated.resources.place_climate
import unitto.core.common.generated.resources.place_climate_cold_desert
import unitto.core.common.generated.resources.place_climate_cold_steppe
import unitto.core.common.generated.resources.place_climate_continental
import unitto.core.common.generated.resources.place_climate_hot_continental
import unitto.core.common.generated.resources.place_climate_hot_desert
import unitto.core.common.generated.resources.place_climate_hot_steppe
import unitto.core.common.generated.resources.place_climate_humid_subtropical
import unitto.core.common.generated.resources.place_climate_ice_cap
import unitto.core.common.generated.resources.place_climate_mediterranean
import unitto.core.common.generated.resources.place_climate_oceanic
import unitto.core.common.generated.resources.place_climate_subarctic
import unitto.core.common.generated.resources.place_climate_subpolar_oceanic
import unitto.core.common.generated.resources.place_climate_subtropical_dry_winter
import unitto.core.common.generated.resources.place_climate_tropical_monsoon
import unitto.core.common.generated.resources.place_climate_tropical_rainforest
import unitto.core.common.generated.resources.place_climate_tropical_savanna
import unitto.core.common.generated.resources.place_climate_tundra
import unitto.core.common.generated.resources.place_day_length
import unitto.core.common.generated.resources.place_drag_hint
import unitto.core.common.generated.resources.place_dry_month
import unitto.core.common.generated.resources.place_eclipse_annular
import unitto.core.common.generated.resources.place_eclipse_covered
import unitto.core.common.generated.resources.place_eclipse_none
import unitto.core.common.generated.resources.place_eclipse_partial
import unitto.core.common.generated.resources.place_eclipse_penumbral
import unitto.core.common.generated.resources.place_eclipse_safety
import unitto.core.common.generated.resources.place_eclipse_searching
import unitto.core.common.generated.resources.place_eclipse_total
import unitto.core.common.generated.resources.place_elevation_note
import unitto.core.common.generated.resources.place_evening
import unitto.core.common.generated.resources.place_golden_hour
import unitto.core.common.generated.resources.place_ground_height
import unitto.core.common.generated.resources.place_high
import unitto.core.common.generated.resources.place_high_tide
import unitto.core.common.generated.resources.place_low
import unitto.core.common.generated.resources.place_low_tide
import unitto.core.common.generated.resources.place_lunar_eclipse
import unitto.core.common.generated.resources.place_meteor_showers
import unitto.core.common.generated.resources.place_moon_distance
import unitto.core.common.generated.resources.place_moon_first_quarter
import unitto.core.common.generated.resources.place_moon_full
import unitto.core.common.generated.resources.place_moon_last_quarter
import unitto.core.common.generated.resources.place_moon_lit
import unitto.core.common.generated.resources.place_moon_new
import unitto.core.common.generated.resources.place_moon_waning_crescent
import unitto.core.common.generated.resources.place_moon_waning_gibbous
import unitto.core.common.generated.resources.place_moon_waxing_crescent
import unitto.core.common.generated.resources.place_moon_waxing_gibbous
import unitto.core.common.generated.resources.place_moonrise
import unitto.core.common.generated.resources.place_moonset
import unitto.core.common.generated.resources.place_morning
import unitto.core.common.generated.resources.place_nautical_twilight
import unitto.core.common.generated.resources.place_neap_tides
import unitto.core.common.generated.resources.place_next_event
import unitto.core.common.generated.resources.place_next_full_moon
import unitto.core.common.generated.resources.place_next_new_moon
import unitto.core.common.generated.resources.place_no_sunrise
import unitto.core.common.generated.resources.place_no_sunset
import unitto.core.common.generated.resources.place_no_tides
import unitto.core.common.generated.resources.place_none_today
import unitto.core.common.generated.resources.place_north
import unitto.core.common.generated.resources.place_panels_day
import unitto.core.common.generated.resources.place_panels_year
import unitto.core.common.generated.resources.place_rain_in
import unitto.core.common.generated.resources.place_real_solar
import unitto.core.common.generated.resources.place_season_december
import unitto.core.common.generated.resources.place_season_june
import unitto.core.common.generated.resources.place_season_march
import unitto.core.common.generated.resources.place_season_september
import unitto.core.common.generated.resources.place_shower_delta_aquariids
import unitto.core.common.generated.resources.place_shower_eta_aquariids
import unitto.core.common.generated.resources.place_shower_geminids
import unitto.core.common.generated.resources.place_shower_hidden
import unitto.core.common.generated.resources.place_shower_leonids
import unitto.core.common.generated.resources.place_shower_lyrids
import unitto.core.common.generated.resources.place_shower_moonlight
import unitto.core.common.generated.resources.place_shower_orionids
import unitto.core.common.generated.resources.place_shower_perseids
import unitto.core.common.generated.resources.place_shower_quadrantids
import unitto.core.common.generated.resources.place_shower_rate
import unitto.core.common.generated.resources.place_shower_ursids
import unitto.core.common.generated.resources.place_snow_share
import unitto.core.common.generated.resources.place_solar_eclipse
import unitto.core.common.generated.resources.place_solar_noon
import unitto.core.common.generated.resources.place_south
import unitto.core.common.generated.resources.place_spring_tides
import unitto.core.common.generated.resources.place_sunrise
import unitto.core.common.generated.resources.place_sunrise_on
import unitto.core.common.generated.resources.place_sunset
import unitto.core.common.generated.resources.place_sunset_on
import unitto.core.common.generated.resources.place_supermoon
import unitto.core.common.generated.resources.place_temperature_note
import unitto.core.common.generated.resources.place_tide_distance
import unitto.core.common.generated.resources.place_tide_note
import unitto.core.common.generated.resources.place_usual_temperature
import unitto.core.common.generated.resources.place_year

@Composable
internal fun PlaceTabContent(tab: PlaceTab, report: PlaceReport, viewModel: PlaceViewModel, units: PlaceUnits) {
  when (tab) {
    PlaceTab.SUN -> SunTab(report, units)
    PlaceTab.MOON -> MoonTab(report, units)
    PlaceTab.SKY -> SkyTab(report, viewModel.eclipses, units)
    PlaceTab.TIDES -> TidesTab(report, units)
    PlaceTab.TEMPERATURE -> TemperatureTab(report, units)
    PlaceTab.RAIN -> RainTab(report, units)
    PlaceTab.ELEVATION -> ElevationTab(report, units)
  }
}

// Sun

@Composable
private fun SunTab(report: PlaceReport, units: PlaceUnits) {
  val sun = report.sun
  val zone = report.zone
  val day = formatDate(report.dayStart + NOON, zone)
  val sunset = sun.sunset
  val sunrise = sun.sunrise
  when {
    sunset != null ->
      BigAnswer(stringResource(Res.string.place_sunset_on, day), formatTime(sunset, zone), sunriseCaption(sunrise, zone))
    sunrise != null -> BigAnswer(stringResource(Res.string.place_sunrise_on, day), formatTime(sunrise, zone), null)
    else -> {
      val isDay = sun.noonAltitude > 0
      BigAnswer(
        stringResource(if (isDay) Res.string.place_no_sunset else Res.string.place_no_sunrise, day),
        units.duration(sun.daylight),
        stringResource(Res.string.place_day_length),
      )
    }
  }
  var selected by remember(report) { mutableIntStateOf(defaultIndex(report)) }
  GraphCard(
    readout = "${formatTime(report.dayStart + selected * CURVE_STEP, zone)} · ${units.degrees(report.sunCurve[selected])}"
  ) {
    CurveGraph(
      listOf(report.sunCurve to MaterialTheme.colorScheme.tertiary),
      selected,
      { selected = it },
      baseline = 0.0,
    )
  }
  Details(
    listOfNotNull(
      sunrise?.let { stringResource(Res.string.place_sunrise) to formatTime(it, zone) },
      stringResource(Res.string.place_solar_noon) to "${formatTime(sun.noon, zone)} · ${units.degrees(sun.noonAltitude)}",
      sunset?.let { stringResource(Res.string.place_sunset) to formatTime(it, zone) },
      stringResource(Res.string.place_day_length) to units.duration(sun.daylight),
      span(stringResource(Res.string.place_golden_hour) + " · " + stringResource(Res.string.place_morning), sun.morningGoldenStart, sun.morningGoldenEnd, zone),
      span(stringResource(Res.string.place_golden_hour) + " · " + stringResource(Res.string.place_evening), sun.goldenHourStart, sun.goldenHourEnd, zone),
      span(stringResource(Res.string.place_blue_hour), sun.goldenHourEnd, sun.blueHourEnd, zone),
      span(stringResource(Res.string.place_civil_twilight), sun.civilDawn, sun.civilDusk, zone),
      span(stringResource(Res.string.place_nautical_twilight), sun.nauticalDawn, sun.nauticalDusk, zone),
      span(stringResource(Res.string.place_astronomical_twilight), sun.astronomicalDawn, sun.astronomicalDusk, zone),
    )
  )
  SolarPanels(report, units)
  RealLifeNote(Res.string.place_real_solar)
}

/** The best direction and angle of a solar panel, for the year and for the chosen date. */
@Composable
private fun SolarPanels(report: PlaceReport, units: PlaceUnits) {
  @Composable
  fun aim(aim: PanelAim): String {
    val direction = if (aim.facesSouth) Res.string.place_south else Res.string.place_north
    return "${stringResource(direction)} · ${units.degrees(aim.tilt)}"
  }
  Details(
    listOfNotNull(
      report.yearPanel?.let { stringResource(Res.string.place_panels_year) to aim(it) },
      report.dayPanel?.let { stringResource(Res.string.place_panels_day) to "${aim(it)} · ${units.number(it.energy, 1)} kWh/m²" },
    )
  )
}

@Composable
private fun sunriseCaption(sunrise: Long?, zone: String): String? =
  sunrise?.let { "${stringResource(Res.string.place_sunrise)} ${formatTime(it, zone)}" }

/** "05:12 – 21:30", or "– 21:30" when it starts the day before. */
private fun span(label: String, start: Long?, end: Long?, zone: String): Pair<String, String>? {
  if (start == null && end == null) return null
  return label to "${start?.let { formatTime(it, zone) }.orEmpty()} – ${end?.let { formatTime(it, zone) }.orEmpty()}"
}

/** Now on today's graph, noon on another day. */
private fun defaultIndex(report: PlaceReport): Int {
  val now = Clock.System.now().toEpochMilliseconds()
  val sinceStart = now - report.dayStart
  return if (sinceStart in 0 until DAY) (sinceStart / CURVE_STEP).toInt() else CURVE_POINTS / 2
}

// Moon

@Composable
private fun MoonTab(report: PlaceReport, units: PlaceUnits) {
  val moon = report.moon
  val zone = report.zone
  Row(
    modifier = Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    MoonDisc(moon.illumination, moon.elongation < HALF_TURN)
    Column {
      Text(
        stringResource(moon.phase.title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Text(
        stringResource(Res.string.place_moon_lit, units.percent(moon.illumination)),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
  val none = stringResource(Res.string.place_none_today)
  Details(
    listOfNotNull(
      stringResource(Res.string.place_moonrise) to (report.moonDay.moonrise?.let { formatTime(it, zone) } ?: none),
      stringResource(Res.string.place_moonset) to (report.moonDay.moonset?.let { formatTime(it, zone) } ?: none),
      stringResource(Res.string.place_next_full_moon) to dateAndTime(report.nextFull, zone),
      stringResource(Res.string.place_next_new_moon) to dateAndTime(report.nextNew, zone),
      stringResource(Res.string.place_moon_distance) to units.distance(moon.distanceKm),
    )
  )
  if (report.isNextFullSuper) Note(stringResource(Res.string.place_supermoon))
}

private fun dateAndTime(millis: Long, zone: String) = "${formatDate(millis, zone)} · ${formatTime(millis, zone)}"

private val MoonPhase.title: StringResource
  get() =
    when (this) {
      MoonPhase.NEW -> Res.string.place_moon_new
      MoonPhase.WAXING_CRESCENT -> Res.string.place_moon_waxing_crescent
      MoonPhase.FIRST_QUARTER -> Res.string.place_moon_first_quarter
      MoonPhase.WAXING_GIBBOUS -> Res.string.place_moon_waxing_gibbous
      MoonPhase.FULL -> Res.string.place_moon_full
      MoonPhase.WANING_GIBBOUS -> Res.string.place_moon_waning_gibbous
      MoonPhase.LAST_QUARTER -> Res.string.place_moon_last_quarter
      MoonPhase.WANING_CRESCENT -> Res.string.place_moon_waning_crescent
    }

// Sky

@Composable
private fun SkyTab(report: PlaceReport, eclipses: EclipseReport?, units: PlaceUnits) {
  val zone = report.zone
  val shower = report.showers.first()
  val next =
    listOfNotNull(
        Triple(shower.peak, stringResource(shower.shower.title), null as String?),
        Triple(report.season.millis, stringResource(report.season.season.title), null),
        eclipses?.lunar?.let { Triple(it.peak, stringResource(Res.string.place_lunar_eclipse), null) },
        eclipses?.solar?.let { Triple(it.peak, stringResource(Res.string.place_solar_eclipse), null) },
      )
      .minBy { it.first }
  BigAnswer(stringResource(Res.string.place_next_event), next.second, dateAndTime(next.first, zone))

  SectionTitle(stringResource(Res.string.place_meteor_showers))
  Details(
    report.showers.map { event ->
      val notes =
        listOfNotNull(
          stringResource(Res.string.place_shower_rate, event.shower.hourlyRate.toString()),
          if (event.radiantMaxAltitude <= 0) stringResource(Res.string.place_shower_hidden) else null,
          if (event.moonIllumination > BRIGHT_MOON) stringResource(Res.string.place_shower_moonlight) else null,
        )
      stringResource(event.shower.title) to (formatDate(event.peak, zone) + "\n" + notes.joinToString("\n"))
    } + (stringResource(report.season.season.title) to dateAndTime(report.season.millis, zone))
  )

  SectionTitle(stringResource(Res.string.place_lunar_eclipse))
  EclipseDetails(eclipses, eclipses?.lunar, zone, units)
  SectionTitle(stringResource(Res.string.place_solar_eclipse))
  EclipseDetails(eclipses, eclipses?.solar, zone, units)
  Note(stringResource(Res.string.place_eclipse_safety))
}

@Composable
private fun EclipseDetails(eclipses: EclipseReport?, eclipse: Eclipse?, zone: String, units: PlaceUnits) {
  val text =
    when {
      eclipses == null -> stringResource(Res.string.place_eclipse_searching)
      eclipse == null -> stringResource(Res.string.place_eclipse_none)
      else -> {
        val covered =
          if (eclipse.isSolar) "\n" + stringResource(Res.string.place_eclipse_covered, units.percent(eclipse.magnitude)) else ""
        dateAndTime(eclipse.peak, zone) + covered
      }
    }
  val kind = eclipse?.kind?.let { stringResource(it.title) }.orEmpty()
  Details(listOf(kind to text))
}

private val EclipseKind.title: StringResource
  get() =
    when (this) {
      EclipseKind.PENUMBRAL -> Res.string.place_eclipse_penumbral
      EclipseKind.PARTIAL -> Res.string.place_eclipse_partial
      EclipseKind.TOTAL -> Res.string.place_eclipse_total
      EclipseKind.ANNULAR -> Res.string.place_eclipse_annular
    }

private val MeteorShower.title: StringResource
  get() =
    when (this) {
      MeteorShower.QUADRANTIDS -> Res.string.place_shower_quadrantids
      MeteorShower.LYRIDS -> Res.string.place_shower_lyrids
      MeteorShower.ETA_AQUARIIDS -> Res.string.place_shower_eta_aquariids
      MeteorShower.DELTA_AQUARIIDS -> Res.string.place_shower_delta_aquariids
      MeteorShower.PERSEIDS -> Res.string.place_shower_perseids
      MeteorShower.ORIONIDS -> Res.string.place_shower_orionids
      MeteorShower.LEONIDS -> Res.string.place_shower_leonids
      MeteorShower.GEMINIDS -> Res.string.place_shower_geminids
      MeteorShower.URSIDS -> Res.string.place_shower_ursids
    }

private val Season.title: StringResource
  get() =
    when (this) {
      Season.MARCH_EQUINOX -> Res.string.place_season_march
      Season.JUNE_SOLSTICE -> Res.string.place_season_june
      Season.SEPTEMBER_EQUINOX -> Res.string.place_season_september
      Season.DECEMBER_SOLSTICE -> Res.string.place_season_december
    }

// Tides

@Composable
private fun TidesTab(report: PlaceReport, units: PlaceUnits) {
  val tides = report.tides
  if (tides == null) {
    Note(stringResource(Res.string.place_no_tides))
    return
  }
  val zone = report.zone
  val now = Clock.System.now().toEpochMilliseconds()
  val high = tides.extremes.filter { it.isHigh }.let { highs -> highs.firstOrNull { it.millis >= now } ?: highs.firstOrNull() }
  if (high != null) {
    BigAnswer(
      stringResource(Res.string.place_high_tide),
      formatTime(high.millis, zone),
      units.tideHeight(high.height) + " · " +
        stringResource(if (tides.isSpring) Res.string.place_spring_tides else Res.string.place_neap_tides),
    )
  }
  var selected by remember(report) { mutableIntStateOf(defaultIndex(report)) }
  GraphCard(
    readout = "${formatTime(report.dayStart + selected * CURVE_STEP, zone)} · ${units.tideHeight(tides.curve[selected])}"
  ) {
    CurveGraph(listOf(tides.curve to MaterialTheme.colorScheme.primary), selected, { selected = it }, baseline = 0.0)
  }
  Details(
    tides.extremes.map {
      stringResource(if (it.isHigh) Res.string.place_high_tide else Res.string.place_low_tide) to
        "${formatTime(it.millis, zone)} · ${units.tideHeight(it.height)}"
    }
  )
  if (tides.distanceKm > NEAR_SEA_KM) Note(stringResource(Res.string.place_tide_distance, units.distance(tides.distanceKm)))
  Note(stringResource(Res.string.place_tide_note))
}

// Temperature

@Composable
private fun TemperatureTab(report: PlaceReport, units: PlaceUnits) {
  val (low, high) = report.dayTemperature(report.date)
  BigAnswer(
    stringResource(Res.string.place_usual_temperature, formatDate(report.dayStart + NOON, report.zone)),
    units.temperatureRange(low, high),
    null,
  )
  val year = report.date.year
  val start = LocalDate(year, 1, 1)
  val days = remember(report) { (0 until WEEKS).map { start.plus(it * DAYS_PER_WEEK, DateTimeUnit.DAY) } }
  val values = remember(report) { days.map { report.dayTemperature(it) } }
  var selected by remember(report) { mutableIntStateOf((report.date.dayOfYear - 1) / DAYS_PER_WEEK) }
  val day = days[selected.coerceIn(days.indices)]
  val (dayLow, dayHigh) = values[selected.coerceIn(values.indices)]
  GraphCard(readout = "${formatDate(dayStart(day, report.zone) + NOON, report.zone)} · ${units.temperatureRange(dayLow, dayHigh)}") {
    CurveGraph(
      listOf(
        values.map { it.second } to MaterialTheme.colorScheme.error,
        values.map { it.first } to MaterialTheme.colorScheme.primary,
      ),
      selected,
      { selected = it },
    )
  }
  Details(
    listOf(
      stringResource(Res.string.place_low) to units.temperature(low),
      stringResource(Res.string.place_high) to units.temperature(high),
    )
  )
  Note(stringResource(Res.string.place_temperature_note, units.elevation(report.elevation)))
}

// Rain

@Composable
private fun RainTab(report: PlaceReport, units: PlaceUnits) {
  var month by remember(report) { mutableIntStateOf(report.date.month.ordinal) }
  val rain = report.precipitation[month]
  val mean = report.monthMean(month)
  val caption =
    listOfNotNull(
        if (isDryMonth(rain, mean)) stringResource(Res.string.place_dry_month) else null,
        snowShare(mean).takeIf { it > 0 }?.let { stringResource(Res.string.place_snow_share, units.percent(it)) },
      )
      .joinToString(" · ")
      .ifEmpty { null }
  BigAnswer(stringResource(Res.string.place_rain_in, monthName(month + 1)), units.rain(rain), caption)
  val labels = remember { (1..MONTHS).map { monthName(it).firstOrNull()?.toString() ?: " " } }
  GraphCard(readout = null) { MonthBars(report.precipitation, labels, month) { month = it } }
  Details(
    listOf(
      stringResource(Res.string.place_year) to units.rain(report.precipitation.sum()),
      stringResource(Res.string.place_climate) to "${stringResource(report.climate.title)} (${report.climate.code})",
    )
  )
}

private val Climate.title: StringResource
  get() =
    when (this) {
      Climate.TROPICAL_RAINFOREST -> Res.string.place_climate_tropical_rainforest
      Climate.TROPICAL_MONSOON -> Res.string.place_climate_tropical_monsoon
      Climate.TROPICAL_SAVANNA -> Res.string.place_climate_tropical_savanna
      Climate.HOT_DESERT -> Res.string.place_climate_hot_desert
      Climate.COLD_DESERT -> Res.string.place_climate_cold_desert
      Climate.HOT_STEPPE -> Res.string.place_climate_hot_steppe
      Climate.COLD_STEPPE -> Res.string.place_climate_cold_steppe
      Climate.MEDITERRANEAN -> Res.string.place_climate_mediterranean
      Climate.SUBTROPICAL_DRY_WINTER -> Res.string.place_climate_subtropical_dry_winter
      Climate.HUMID_SUBTROPICAL -> Res.string.place_climate_humid_subtropical
      Climate.OCEANIC -> Res.string.place_climate_oceanic
      Climate.SUBPOLAR_OCEANIC -> Res.string.place_climate_subpolar_oceanic
      Climate.HOT_CONTINENTAL -> Res.string.place_climate_hot_continental
      Climate.CONTINENTAL -> Res.string.place_climate_continental
      Climate.SUBARCTIC -> Res.string.place_climate_subarctic
      Climate.TUNDRA -> Res.string.place_climate_tundra
      Climate.ICE_CAP -> Res.string.place_climate_ice_cap
    }

// Elevation

@Composable
private fun ElevationTab(report: PlaceReport, units: PlaceUnits) {
  val atSea = report.elevation <= 0 && report.tides != null
  BigAnswer(
    stringResource(Res.string.place_ground_height),
    if (atSea) stringResource(Res.string.place_at_sea) else units.elevation(report.elevation),
    formatPoint(report.point),
  )
  Note(stringResource(Res.string.place_elevation_note))
}

// Shared pieces

/** The answer in big on the primary color, what it is above, more below. */
/** How far real life can be from the numbers above. */
@Composable
internal fun RealLifeNote(text: StringResource) {
  Text(
    stringResource(text),
    modifier = Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun BigAnswer(label: String, value: String, caption: String?) {
  val color = MaterialTheme.colorScheme.onPrimaryContainer
  Column(
    modifier = Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(label, style = MaterialTheme.typography.titleMedium, color = color)
    Text(value, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = color)
    if (caption != null) Text(caption, style = MaterialTheme.typography.bodyLarge, color = color)
  }
}

@Composable
private fun GraphCard(readout: String?, graph: @Composable () -> Unit) {
  Column(
    modifier = Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (readout != null) {
      Text(readout, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
    graph()
    if (readout != null) {
      Text(
        stringResource(Res.string.place_drag_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
      )
    }
  }
}

/** Label on the left, value on the right, one row each. */
@Composable
private fun Details(rows: List<Pair<String, String>>) {
  if (rows.isEmpty()) return
  Column(
    modifier =
      Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    rows.forEach { (label, value) ->
      Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
          value,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.End,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Note(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
  Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
}

private val CARD_SHAPE = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
private const val DAY = 86_400_000L
private const val WEEKS = 53
private const val DAYS_PER_WEEK = 7
private const val MONTHS = 12
private const val BRIGHT_MOON = 0.5
private const val HALF_TURN = 180.0
private const val NEAR_SEA_KM = 5.0
