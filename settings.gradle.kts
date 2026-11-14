/*
 * Unitto is a calculator for Android
 * Copyright (c) 2024-2026 Elshan Agaev
 * Copyright (c) 2026 Calculator-Pascaline (modifications)
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

pluginManagement {
  includeBuild("build-logic")
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Pascaline"

with(this) {
  include(":sharedApp")
  include(":androidApp")
  include(":core:common")
  include(":core:model")
  include(":core:themmo")
  include(":core:navigation")
  include(":core:designsystem")
  include(":core:ui")
  include(":core:database")
  include(":core:data")
  include(":core:evaluatto")
  include(":core:licenses")
  include(":core:backup")
  include(":core:datastore")
  include(":feature:bodymass")
  include(":feature:calculator")
  include(":feature:converter")
  include(":feature:datecalculator")
  include(":feature:glance")
  include(":feature:settings")
  include(":kt-math")
}
