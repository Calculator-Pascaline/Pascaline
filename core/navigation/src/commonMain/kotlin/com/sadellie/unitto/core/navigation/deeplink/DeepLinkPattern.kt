/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025 Elshan Agaev
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

package com.sadellie.unitto.core.navigation.deeplink

import androidx.navigation3.runtime.NavKey
import com.eygraber.uri.Uri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind

/**
 * Parse a supported deeplink and stores its metadata as a easily readable format
 *
 * The following notes applies specifically to this particular sample implementation:
 *
 * The supported deeplink is expected to be built from a serializable backstack key [T] that
 * supports deeplink. This means that if this deeplink contains any arguments (path or query), the
 * argument name must match any of [T] member field name.
 *
 * One [DeepLinkPattern] should be created for each supported deeplink. This means if [T] supports
 * two deeplink patterns:
 * ```
 *  val deeplink1 = www.nav3recipes.com/home
 *  val deeplink2 = www.nav3recipes.com/profile/{userId}
 *  ```
 *
 * Then two [DeepLinkPattern] should be created
 *
 * ```
 * val parsedDeeplink1 = DeepLinkPattern(T.serializer(), deeplink1)
 * val parsedDeeplink2 = DeepLinkPattern(T.serializer(), deeplink2)
 * ```
 *
 * This implementation assumes a few things:
 * 1. all path arguments are required/non-nullable - partial path matches will be considered a
 *    non-match
 * 2. all query arguments are optional by way of nullable/has default value
 *
 * @param T the backstack key type that supports the deeplinking of [uriPattern]
 * @param serializer the serializer of [T]
 * @param uriPattern the supported deeplink's uri pattern, i.e. "abc.com/home/{pathArg}"
 */
internal class DeepLinkPattern<T : NavKey>(val serializer: KSerializer<T>, val uriPattern: Uri) {
  /** Help differentiate if a path segment is an argument or a static value */
  private val regexPatternFillIn = Regex("\\{(.+?)\\}")

  /**
   * The path as a list of [PathSegment]
   *
   * order matters here - path segments need to match in value and order when matching requested
   * deeplink to supported deeplink
   */
  val pathSegments: List<PathSegment> = uriPattern.pathSegments.map(::parsePathSegment)

  /**
   * Supported queries as a map of queryParameterNames to [TypeParser]
   *
   * This will be used later on to parse a provided query value into the correct KType
   */
  val queryValueParsers: Map<String, TypeParser> =
    uriPattern.getQueryParameterNames().associateWith(::elementParser)

  private fun parsePathSegment(segment: String): PathSegment {
    // the arg name is the string value within the curly braces
    val argName = regexPatternFillIn.find(segment)?.groupValues?.get(1)
    // not a path arg, just a static string path segment
    if (argName == null) return PathSegment(segment, false, getTypeParser(PrimitiveKind.STRING))
    return PathSegment(argName, true, elementParser(argName))
  }

  /** Parser for the primitive type of the argument [name] of [T]. */
  private fun elementParser(name: String): TypeParser {
    val elementIndex = serializer.descriptor.getElementIndex(name)
    return getTypeParser(serializer.descriptor.getElementDescriptor(elementIndex).kind)
  }

  /** Metadata about a supported path segment */
  class PathSegment(val stringValue: String, val isParamArg: Boolean, val typeParser: TypeParser)
}

/** Parses a String into a Serializable Primitive */
private typealias TypeParser = (String) -> Any

private fun getTypeParser(kind: SerialKind): TypeParser {
  return when (kind) {
    PrimitiveKind.STRING -> Any::toString
    PrimitiveKind.INT -> String::toInt
    PrimitiveKind.BOOLEAN -> String::toBoolean
    PrimitiveKind.BYTE -> String::toByte
    PrimitiveKind.CHAR -> String::toCharArray
    PrimitiveKind.DOUBLE -> String::toDouble
    PrimitiveKind.FLOAT -> String::toFloat
    PrimitiveKind.LONG -> String::toLong
    PrimitiveKind.SHORT -> String::toShort
    else ->
      throw IllegalArgumentException(
        "Unsupported argument type of SerialKind:$kind. The argument type must be a Primitive."
      )
  }
}
