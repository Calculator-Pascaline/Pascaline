/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025 Elshan Agaev
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
import co.touchlab.kermit.Logger
import kotlinx.serialization.KSerializer

internal class DeepLinkMatcher<T : NavKey>(
  val request: DeepLinkRequest,
  val deepLinkPattern: DeepLinkPattern<T>,
) {
  /**
   * Match a [DeepLinkRequest] to a [DeepLinkPattern].
   *
   * Returns a [DeepLinkMatchResult] if this matches the pattern, returns null otherwise
   */
  fun match(): DeepLinkMatchResult<T>? {
    if (request.pathSegments.size != deepLinkPattern.pathSegments.size) return null
    // exact match (url does not contain any arguments)
    if (request.uri == deepLinkPattern.uriPattern) return DeepLinkMatchResult(deepLinkPattern.serializer, mapOf())
    val args = mutableMapOf<String, Any>()
    // provide the serializer of the matching key and map of arg names to parsed arg values
    return if (matchPath(args) && matchQueries(args)) DeepLinkMatchResult(deepLinkPattern.serializer, args) else null
  }

  /** Every path segment matches, the path arguments are put in [args]. */
  private fun matchPath(args: MutableMap<String, Any>): Boolean =
    request.pathSegments
      .asSequence()
      // zip to compare the two objects side by side, order matters here so we
      // need to make sure the compared segments are at the same position within the url
      .zip(deepLinkPattern.pathSegments.asSequence())
      .all { (requestedSegment, candidateSegment) ->
        if (candidateSegment.isParamArg) {
          // if the potential match expects a path arg for this segment, try to parse the
          // requested segment into the expected type
          val parsedValue =
            try {
              candidateSegment.typeParser.invoke(requestedSegment)
            } catch (e: IllegalArgumentException) {
              Logger.e(TAG_LOG_ERROR, e) { "Failed to parse path value:[$requestedSegment]." }
              null
            }
          if (parsedValue != null) args[candidateSegment.stringValue] = parsedValue
          parsedValue != null
        } else {
          // if it's path arg is not the expected type, its not a match
          requestedSegment == candidateSegment.stringValue
        }
      }

  /** Every query (if any) is known and parsed, into [args]. */
  private fun matchQueries(args: MutableMap<String, Any>): Boolean =
    request.queries.all { (name, value) ->
      val queryStringParser = deepLinkPattern.queryValueParsers[name]
      if (queryStringParser == null) Logger.e(TAG_LOG_ERROR) { "Failed to find parser for name:[$name] value:[$value]." }
      val queryParsedValue =
        try {
          queryStringParser?.invoke(value)
        } catch (e: IllegalArgumentException) {
          Logger.e(TAG_LOG_ERROR, e) { "Failed to parse query name:[$name] value:[$value]." }
          null
        }
      if (queryParsedValue != null) args[name] = queryParsedValue
      queryParsedValue != null
    }
}

/**
 * Created when a requested deeplink matches with a supported deeplink
 *
 * @param [T] the backstack key associated with the deeplink that matched with the requested
 *   deeplink
 * @param serializer serializer for [T]
 * @param args The map of argument name to argument value. The value is expected to have already
 *   been parsed from the raw url string back into its proper KType as declared in [T]. Includes
 *   arguments for all parts of the uri - path, query, etc.
 */
data class DeepLinkMatchResult<T : NavKey>(
  val serializer: KSerializer<T>,
  val args: Map<String, Any>,
)

const val TAG_LOG_ERROR = "Nav3RecipesDeepLink"
