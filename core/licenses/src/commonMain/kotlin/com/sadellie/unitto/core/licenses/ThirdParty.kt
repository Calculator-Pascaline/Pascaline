/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2025 Elshan Agaev
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

package com.sadellie.unitto.core.licenses

data class ThirdParty(
  val name: String,
  val dev: String,
  val website: String,
  val license: String,
  val description: String,
) {
  companion object {
    fun allThirdParty(): List<ThirdParty> =
      listOf(
        ThirdParty(
          name = "Lato",
          dev = "Łukasz Dziedzic",
          website = "https://fonts.google.com/specimen/Lato/about",
          license = "Open Font License",
          description =
            "Lato is a sans serif typeface family started in the summer of 2010 by Warsaw-based designer Łukasz Dziedzic (“Lato” means “Summer” in Polish). In December 2010 the Lato family was published under the Open Font License by his foundry tyPoland, with support from Google.",
        ),
        ThirdParty(
          name = "Noto Sans",
          dev = "The Noto Project Authors",
          website = "https://notofonts.github.io",
          license = "Open Font License",
          description =
            "Noto Sans, Noto Sans Math, Noto Sans Devanagari and Noto Sans SC. The web app uses them for the characters the browser can't show otherwise.",
        ),
        ThirdParty(
          name = "Core Kotlin Extensions",
          dev = "The Android Open Source Project",
          website = "https://developer.android.com/jetpack/androidx/releases/core",
          license = "Apache-2.0",
          description = "Kotlin extensions for 'core' artifact",
        ),
        ThirdParty(
          name = "Compose UI primitives",
          dev = "The Android Open Source Project",
          website = "https://developer.android.com/jetpack/androidx/releases/compose-ui",
          license = "Apache-2.0",
          description =
            "Compose UI primitives. This library contains the primitives that form the Compose UI Toolkit, such as drawing, measurement and layout.",
        ),
        ThirdParty(
          name = "Compose Navigation",
          dev = "The Android Open Source Project",
          website = "https://developer.android.com/jetpack/androidx/releases/navigation",
          license = "Apache-2.0",
          description = "Compose integration with Navigation",
        ),
        ThirdParty(
          name = "Compose Material3 Components",
          dev = "The Android Open Source Project",
          website = "https://developer.android.com/jetpack/androidx/releases/compose-material3",
          license = "Apache-2.0",
          description = "Compose Material You Design Components library",
        ),
        ThirdParty(
          name = "Material Symbols",
          dev = "Google Fonts",
          website = "https://fonts.google.com/",
          license = "Apache-2.0",
          description = "Icons",
        ),
        ThirdParty(
          name = "Android Preferences DataStore",
          dev = "The Android Open Source Project",
          website = "https://developer.android.com/jetpack/androidx/releases/datastore",
          license = "Apache-2.0",
          description = "Android Preferences DataStore",
        ),
        ThirdParty(
          name = "Compose Tooling API",
          dev = "The Android Open Source Project",
          website = "https://developer.android.com/jetpack/androidx/releases/compose-ui",
          license = "Apache-2.0",
          description =
            "Compose tooling library API. This library provides the API required to declare @Preview composables in user apps.",
        ),
        ThirdParty(
          name = "Reorderable",
          dev = "Calvin Liang (Calvin-LL)",
          website = "https://github.com/Calvin-LL/Reorderable",
          license = "Apache-2.0",
          description =
            "Reorder items in Lists and Grids in Jetpack Compose and Compose Multiplatform with drag and drop",
        ),
        ThirdParty(
          name = "m3color",
          dev = "Kyant0",
          website = "https://github.com/Kyant0/m3color",
          license = "Apache-2.0",
          description = "Material 3 color palette Java library",
        ),
        ThirdParty(
          name = "big-math",
          dev = "eobermuhlner (Eric Obermühlner)",
          website = "https://github.com/eobermuhlner/big-math",
          license = "MIT",
          description =
            "Advanced Java BigDecimal math functions (pow, sqrt, log, sin, …) using arbitrary precision",
        ),
      ) + placeData()

    /** Data of the Place app. */
    private fun placeData(): List<ThirdParty> =
      listOf(
        ThirdParty(
          name = "Trail Sense",
          dev = "Kyle Corry",
          website = "https://github.com/kylecorry31/Trail-Sense",
          license = "MIT",
          description =
            "The Place app reads the climate, tide, elevation and land data that Trail Sense bundles. Copyright (c) 2020 Kyle Corry. Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files, to deal in the Software without restriction, subject to including this notice. The software is provided \"as is\", without warranty of any kind.",
        ),
        ThirdParty(
          name = "MERRA-2 and ERA5",
          dev = "NASA GMAO, Copernicus Climate Change Service (ECMWF)",
          website = "https://gmao.gsfc.nasa.gov/reanalysis/MERRA-2/",
          license = "Public domain, Copernicus licence",
          description =
            "Daily highs and lows of 1991–2020 (MERRA-2) and monthly precipitation (ERA5), as prepared by Trail Sense. Contains modified Copernicus Climate Change Service information.",
        ),
        ThirdParty(
          name = "EOT20",
          dev = "Hart-Davis et al., DGFI-TUM",
          website = "https://doi.org/10.17882/79489",
          license = "CC BY 4.0",
          description = "Global ocean tide model, its constituents as prepared by Trail Sense for tide estimates.",
        ),
        ThirdParty(
          name = "ETOPO 2022",
          dev = "NOAA National Centers for Environmental Information",
          website = "https://www.ncei.noaa.gov/products/etopo-global-relief-model",
          license = "Public domain",
          description = "Global relief model, the ground height of the Place app as prepared by Trail Sense.",
        ),
        ThirdParty(
          name = "tz database",
          dev = "IANA",
          website = "https://www.iana.org/time-zones",
          license = "Public domain",
          description = "The cities of zone1970.tab guess the time zone of a point.",
        ),
      )
  }
}
