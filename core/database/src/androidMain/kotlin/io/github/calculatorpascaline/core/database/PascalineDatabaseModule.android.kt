/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2025 Elshan Agaev
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

package io.github.calculatorpascaline.core.database

import androidx.room.Room
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.lazyModule

const val DATABASE_NAME = "pascaline_database"

/** File name used before the rename to Pascaline. Old installs and old backups still use it. */
const val LEGACY_DATABASE_NAME = "unitto_database"

val pascalineDatabaseModule = lazyModule {
  single<PascalineDatabaseAndroid> {
    val appContext = androidContext()
    moveLegacyDatabase(
      from = appContext.getDatabasePath(LEGACY_DATABASE_NAME),
      to = appContext.getDatabasePath(DATABASE_NAME),
    )
    Room.databaseBuilder(
        appContext.applicationContext,
        PascalineDatabaseAndroid::class.java,
        DATABASE_NAME,
      )
      .build()
  }
  factory<RawDao> { get<PascalineDatabaseAndroid>().rawDao() }
  factory<UnitsDao> { get<PascalineDatabaseAndroid>().unitsDao() }
  factory<CalculatorHistoryDao> { get<PascalineDatabaseAndroid>().calculatorHistoryDao() }
  factory<TimeZoneDao> { get<PascalineDatabaseAndroid>().timeZoneDao() }
  factory<CurrencyRatesDao> { get<PascalineDatabaseAndroid>().currencyRatesDao() }
  factory<ConverterWidgetUnitPairDao> { get<PascalineDatabaseAndroid>().converterWidgetUnitsPairDao() }
  factory<AppStatsDao> { get<PascalineDatabaseAndroid>().appStatsDao() }
}

/**
 * Renames the database file of installs from before the rename to Pascaline, with its journal
 * files, so saved history and favourites are kept. Does nothing once the new file exists.
 */
private fun moveLegacyDatabase(from: File, to: File) {
  if (!from.exists() || to.exists()) return
  for (suffix in listOf("", "-wal", "-shm", "-journal")) {
    val old = File(from.path + suffix)
    if (old.exists()) old.renameTo(File(to.path + suffix))
  }
}
