/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2024 Elshan Agaev
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

package com.sadellie.unitto.feature.timezone

import android.icu.util.TimeZone
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.ZoneId
import java.time.ZonedDateTime

/** The same zone for java.time, short ids like "ACT" included. */
@RequiresApi(Build.VERSION_CODES.N)
fun TimeZone.zoneId(): ZoneId = ZoneId.of(this.id, ZoneId.SHORT_IDS)

@RequiresApi(Build.VERSION_CODES.N)
fun TimeZone.timeNow(): ZonedDateTime = ZonedDateTime.now(zoneId())

/**
 * [hour]:[minute] picked on the day of [zoneTime] in [zone], as the same moment in [userZone]. Summer
 * time is the one of that day in each zone.
 */
internal fun pickedInUserZone(
  zoneTime: ZonedDateTime,
  zone: ZoneId,
  hour: Int,
  minute: Int,
  userZone: ZoneId,
): ZonedDateTime =
  zoneTime.toLocalDateTime().withHour(hour).withMinute(minute).atZone(zone).withZoneSameInstant(userZone)
