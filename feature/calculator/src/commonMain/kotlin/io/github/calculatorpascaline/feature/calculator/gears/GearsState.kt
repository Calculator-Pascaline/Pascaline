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

package io.github.calculatorpascaline.feature.calculator.gears

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.github.calculatorpascaline.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.gears_example_bicycle
import pascaline.core.common.generated.resources.gears_example_car
import pascaline.core.common.generated.resources.gears_example_clock
import pascaline.core.common.generated.resources.gears_example_lego
import pascaline.core.common.generated.resources.gears_example_screwdriver
import pascaline.core.common.generated.resources.gears_mode_find
import pascaline.core.common.generated.resources.gears_mode_planetary
import pascaline.core.common.generated.resources.gears_mode_train
import pascaline.core.common.generated.resources.gears_power
import pascaline.core.common.generated.resources.gears_torque

/** What the Gears tool shows: a train of gears, a planetary set, or trains found for a ratio. */
internal enum class GearsMode(val title: StringResource) {
  TRAIN(Res.string.gears_mode_train),
  PLANETARY(Res.string.gears_mode_planetary),
  FIND(Res.string.gears_mode_find),
}

/** What is given at the input: its torque, or the power that turns it. */
internal enum class Drive(val title: StringResource) {
  TORQUE(Res.string.gears_torque),
  POWER(Res.string.gears_power),
}

/** A gear after the input as it is typed. */
internal data class GearEntry(
  val teeth: String,
  val link: Link,
  val starts: String = "1",
  val efficiency: String = "40",
  val pitch: String = "",
)

/**
 * Everything typed in the Gears tool. [given] is the motor when true; when false, the speed and the torque or power
 * are what the output needs, and the motor is worked out.
 */
internal data class GearsInputs(
  val mode: GearsMode = GearsMode.TRAIN,
  val teeth: String = "20",
  val system: PitchSystem = PitchSystem.MODULE,
  val module: String = "2",
  val diametral: String = "12",
  val given: Boolean = true,
  val speed: String = "1500",
  val drive: Drive = Drive.TORQUE,
  val torque: String = "10",
  val power: String = "1.5",
  val efficiency: String = "98",
  // 20 teeth on the motor, a 40 that halves the speed, and a 15 on its shaft that drives a 45: 6 to 1 in all
  val gears: List<GearEntry> =
    listOf(GearEntry("40", Link.MESH), GearEntry("15", Link.SHAFT), GearEntry("45", Link.MESH)),
  val faceWidth: String = "20",
  val material: Material = Material.STEEL,
  val sun: String = "12",
  val ring: String = "48",
  val planets: String = "3",
  val fixed: Member = Member.RING,
  val input: Member = Member.SUN,
  val target: String = "12.5",
  val stages: Int = 2,
  val minTeeth: String = "17",
  val maxTeeth: String = "60",
  val speedUnit: SpeedUnit = SpeedUnit.RPM,
  val units: ForceUnits = ForceUnits.METRIC,
) {
  /** All fields as one line of text, to be kept and read back with [decodeInputs]. */
  fun encode(): String =
    listOf(
        VERSION,
        mode.name,
        teeth,
        system.name,
        module,
        diametral,
        given.toString(),
        speed,
        drive.name,
        torque,
        power,
        efficiency,
        gears.joinToString(GEAR_SEPARATOR) { listOf(it.teeth, it.link.name, it.starts, it.efficiency, it.pitch).joinToString(PART_SEPARATOR) },
        faceWidth,
        material.name,
        sun,
        ring,
        planets,
        fixed.name,
        input.name,
        target,
        stages.toString(),
        minTeeth,
        maxTeeth,
        speedUnit.name,
        units.name,
      )
      .joinToString(FIELD_SEPARATOR)
}

/** Inputs written by [GearsInputs.encode], or null when [text] isn't such a line. */
internal fun decodeInputs(text: String): GearsInputs? {
  val fields = text.split(FIELD_SEPARATOR)
  if (fields.size != FIELD_COUNT || fields[0] != VERSION) return null
  val iterator = fields.drop(1).iterator()
  fun next() = iterator.next()
  return runCatching {
      GearsInputs(
        mode = GearsMode.valueOf(next()),
        teeth = next(),
        system = PitchSystem.valueOf(next()),
        module = next(),
        diametral = next(),
        given = next().toBoolean(),
        speed = next(),
        drive = Drive.valueOf(next()),
        torque = next(),
        power = next(),
        efficiency = next(),
        gears = decodeGears(next()),
        faceWidth = next(),
        material = Material.valueOf(next()),
        sun = next(),
        ring = next(),
        planets = next(),
        fixed = Member.valueOf(next()),
        input = Member.valueOf(next()),
        target = next(),
        stages = next().toInt(),
        minTeeth = next(),
        maxTeeth = next(),
        speedUnit = SpeedUnit.valueOf(next()),
        units = ForceUnits.valueOf(next()),
      )
    }
    .getOrNull()
}

private fun decodeGears(text: String): List<GearEntry> =
  if (text.isEmpty()) emptyList()
  else
    text.split(GEAR_SEPARATOR).map { gear ->
      val parts = gear.split(PART_SEPARATOR)
      GearEntry(teeth = parts[0], link = Link.valueOf(parts[1]), starts = parts[2], efficiency = parts[3], pitch = parts[4])
    }

/** Keeps the inputs when the screen turns or the app is sent away. */
internal val INPUTS_SAVER = Saver<GearsInputs, String>(save = { it.encode() }, restore = { decodeInputs(it) })

/** Trains to start from, each with its own [title]. */
internal enum class Example(val title: StringResource, val inputs: GearsInputs) {
  // A motor on an 8, a 24 that turns a worm, and its wheel of 24: 72 to 1, and the wheel can't turn the motor
  LEGO(
    Res.string.gears_example_lego,
    GearsInputs(
      teeth = "8",
      module = "1",
      speed = "400",
      torque = "0.1",
      efficiency = "95",
      gears = listOf(GearEntry("24", Link.MESH), GearEntry("24", Link.WORM, starts = "1", efficiency = "40")),
    ),
  ),
  // Pedals at 80 rpm with 150 W on a 44 chainring, and a 16 sprocket at the back: 220 rpm at the wheel
  BICYCLE(
    Res.string.gears_example_bicycle,
    GearsInputs(teeth = "44", module = "", speed = "80", drive = Drive.POWER, power = "0.15", gears = listOf(GearEntry("16", Link.BELT))),
  ),
  // The minute hand turns once an hour; 10 → 30 and 8 → 32 turn the hour hand twelve times slower
  CLOCK(
    Res.string.gears_example_clock,
    GearsInputs(
      teeth = "10",
      module = "0.5",
      speed = "0.0166667",
      torque = "0.001",
      efficiency = "99",
      gears = listOf(GearEntry("30", Link.MESH), GearEntry("8", Link.SHAFT), GearEntry("32", Link.MESH)),
    ),
  ),
  // An engine at 3000 rpm and 200 N·m in first gear, 13 → 46, then the final drive with bigger teeth, 13 → 51:
  // about 14 to 1, and teeth wide enough for the torque
  CAR(
    Res.string.gears_example_car,
    GearsInputs(
      teeth = "13",
      module = "4",
      speed = "3000",
      torque = "200",
      efficiency = "97",
      gears = listOf(GearEntry("46", Link.MESH), GearEntry("13", Link.SHAFT, pitch = "6"), GearEntry("51", Link.MESH)),
      faceWidth = "50",
      material = Material.HARDENED_STEEL,
    ),
  ),
  // A sun of 12 in a ring of 48 that is held: the carrier turns five times slower than the motor
  SCREWDRIVER(
    Res.string.gears_example_screwdriver,
    GearsInputs(mode = GearsMode.PLANETARY, speed = "1200", torque = "0.5", efficiency = "97"),
  ),
}

/** A train kept under a [name]. */
internal data class SavedTrain(val name: String, val inputs: GearsInputs)

/** Trains the user kept, saved in the app's data so that they stay after the app closes, and in backups. */
internal class SavedTrains(private val preferences: UserPreferencesRepository, scope: CoroutineScope) {
  var trains by mutableStateOf<List<SavedTrain>?>(null)
    private set

  init {
    scope.launch {
      trains = decode(preferences.appData(KEY).first())
      snapshotFlow { trains }.filterNotNull().drop(1).collect { preferences.updateAppData(KEY, encode(it)) }
    }
  }

  /** Keeps [inputs] under [name], instead of a train that had the same name. */
  fun save(name: String, inputs: GearsInputs) {
    val current = trains ?: return
    trains = current.filterNot { it.name == name } + SavedTrain(name, inputs)
  }

  fun delete(train: SavedTrain) {
    trains = trains?.filterNot { it == train }
  }

  private fun encode(trains: List<SavedTrain>): String =
    trains.joinToString(TRAIN_SEPARATOR) { it.name.replace(NAME_SEPARATOR, " ") + NAME_SEPARATOR + it.inputs.encode() }

  private fun decode(text: String): List<SavedTrain> =
    text.split(TRAIN_SEPARATOR).mapNotNull { line ->
      val name = line.substringBefore(NAME_SEPARATOR, "")
      decodeInputs(line.substringAfter(NAME_SEPARATOR, ""))?.let { SavedTrain(name, it) }
    }
}

@Composable
internal fun rememberSavedTrains(): SavedTrains {
  val preferences = koinInject<UserPreferencesRepository>()
  val scope = rememberCoroutineScope()
  return remember(preferences) { SavedTrains(preferences, scope) }
}

private const val VERSION = "1"
private const val FIELD_COUNT = 26
private const val KEY = "gears_saved"
// Control characters that can't be typed, so that names and numbers never hold them
private const val FIELD_SEPARATOR = "\u001F"
private const val GEAR_SEPARATOR = "\u001E"
private const val PART_SEPARATOR = "\u001D"
private const val NAME_SEPARATOR = "\u001C"
private const val TRAIN_SEPARATOR = "\n"
