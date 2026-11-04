/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2026 Elshan Agaev
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

plugins {
  id("unitto.multiplatform.library")
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.compose)
}

kotlin {
  android { namespace = "com.sadellie.unitto.shared" }
  sourceSets.commonMain.dependencies {
    implementation(libs.org.jetbrains.compose.material3.material3)
    implementation(libs.org.jetbrains.compose.components.components.resources)
    implementation(libs.org.jetbrains.compose.ui.ui.tooling.preview)
    implementation(libs.org.jetbrains.compose.material3.material3.window.size)
    implementation(libs.org.jetbrains.androidx.navigation3.navigation3.ui)
    implementation(libs.org.jetbrains.androidx.lifecycle.lifecycle.viewmodel.navigation3)
    implementation(libs.com.eygraber.uri.kmp)
    implementation(project.dependencies.platform(libs.io.insert.koin.koin.bom))
    implementation(libs.io.insert.koin.koin.core)
    implementation(libs.io.insert.koin.koin.compose.navigation3)
    implementation(project(":core:common"))
    implementation(project(":core:themmo"))
    implementation(project(":core:navigation"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(libs.org.jetbrains.kotlinx.kotlinx.datetime)
    implementation(project(":feature:calculator"))
    implementation(project(":feature:converter"))
    implementation(project(":feature:bodymass"))
    implementation(project(":feature:datecalculator"))
    implementation(project(":feature:timezone"))
    implementation(project(":feature:settings"))
  }
  sourceSets.androidMain.dependencies {
    implementation(project(":feature:glance"))
    implementation(libs.io.insert.koin.koin.androidx.startup)
    implementation(libs.io.insert.koin.koin.core.coroutines)
  }
}

// The web app's fonts are in wasmJsMain
compose.resources { packageOfResClass = "com.sadellie.unitto.shared.generated.resources" }

// Offline web app: the service worker gets the list of files to keep and a new cache version
val wasmPrecache =
  tasks.register("wasmPrecache") {
    group = "pwa"
    description = "Puts the built files and a cache version into sw.js"
    val distDir = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
    doLast {
      val dist = distDir.get().asFile
      val sw = dist.resolve("sw.js")
      if (!sw.exists()) throw GradleException("sw.js not found in $dist")
      val assets =
        dist
          .walkTopDown()
          .filter { it.isFile && it.name != "sw.js" && !it.isHidden }
          // Source map and licence are not needed to run
          .filter { !it.name.endsWith(".map") && !it.name.endsWith(".LICENSE.txt") }
          .map { it.relativeTo(dist).path.replace(File.separatorChar, '/') }
          .sorted()
          .joinToString(",") { "\"$it\"" }
      val placeholder = "[/*__PRECACHED_ASSETS__*/]"
      val content = sw.readText()
      if (placeholder !in content) throw GradleException("$placeholder not found in sw.js")
      sw.writeText(
        content
          .replace(placeholder, "[\"./\",$assets]")
          .replace("{VERSION}", System.currentTimeMillis().toString())
      )
    }
  }

tasks.matching { it.name == "wasmJsBrowserDistribution" }.configureEach { finalizedBy(wasmPrecache) }
