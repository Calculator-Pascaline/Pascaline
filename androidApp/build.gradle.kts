/*
 * Unitto is a calculator for Android
 * Copyright (c) 2026 Elshan Agaev
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

import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose)
  alias(libs.plugins.compose.compiler)
}

kotlin {
  target { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }
  dependencies {
    implementation(project(":sharedApp"))
    implementation(project(":core:themmo"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(libs.androidx.core.core.ktx)
    implementation(libs.androidx.appcompat.appcompat)
    implementation(libs.com.eygraber.uri.kmp)
    implementation(libs.org.jetbrains.compose.material3.material3)
    implementation(libs.org.jetbrains.compose.material3.material3.window.size)
    implementation(libs.androidx.lifecycle.lifecycle.runtime.compose)
    implementation(libs.org.jetbrains.androidx.navigation3.navigation3.ui)
    implementation(project.dependencies.platform(libs.io.insert.koin.koin.bom))
    implementation(libs.io.insert.koin.koin.core)
  }
}

// Release key, kept out of git: storeFile, storePassword, keyAlias and keyPassword
val keystoreProperties =
  rootProject
    .file("keystore.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }

android {
  namespace = "com.sadellie.unitto"

  signingConfigs {
    if (keystoreProperties != null) {
      create("release") {
        storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
        storePassword = keystoreProperties.getProperty("storePassword")
        keyAlias = keystoreProperties.getProperty("keyAlias")
        keyPassword = keystoreProperties.getProperty("keyPassword")
      }
    }
  }
  compileSdk = 36

  defaultConfig {
    applicationId = "io.github.calculatorpascaline"
    minSdk = 23
    targetSdk = 36
    versionCode = libs.versions.versionCode.get().toInt()
    versionName = libs.versions.versionName.get()
    androidResources.localeFilters +=
      setOf(
        "en",
        "en-rGB",
        "de",
        "es",
        "fr",
        "hu",
        "in",
        "it",
        "nl",
        "pt-rBR",
        "ru",
        "tr",
        "zh-rCN",
      )
  }

  buildTypes {
    debug {
      isDebuggable = true
      isMinifyEnabled = false
      isShrinkResources = false
      applicationIdSuffix = ".debug"
    }
    release {
      initWith(getByName("debug"))
      isDebuggable = false
      isMinifyEnabled = true
      isShrinkResources = true
      applicationIdSuffix = ""
      // Never the debug key: without keystore.properties the APK is unsigned
      signingConfig = signingConfigs.findByName("release")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  buildFeatures {
    compose = true
    aidl = false
    shaders = false
    buildConfig = false
    resValues = false
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    isCoreLibraryDesugaringEnabled = true
  }
  kotlin.jvmToolchain(11)
  composeOptions.kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
  packaging.resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
}

dependencies { coreLibraryDesugaring(libs.com.android.tools.desugar.jdk.libs) }
