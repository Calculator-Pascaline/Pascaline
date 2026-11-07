/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025-2026 Elshan Agaev
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

package com.sadellie.unitto

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sadellie.unitto.core.datastore.PrefKeys
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.designsystem.unittoFadeIn
import com.sadellie.unitto.core.designsystem.unittoFadeOut
import com.sadellie.unitto.core.navigation.DRAWER_LAYOUT_KEY
import com.sadellie.unitto.core.navigation.DrawerItem
import com.sadellie.unitto.core.navigation.DrawerLayout
import com.sadellie.unitto.core.navigation.HomeDrawerItem
import com.sadellie.unitto.core.navigation.HomeStartRoute
import com.sadellie.unitto.core.navigation.LocalNavigator
import com.sadellie.unitto.core.navigation.NamedTool
import com.sadellie.unitto.core.navigation.Navigator
import com.sadellie.unitto.core.navigation.ORDER_SEPARATOR
import com.sadellie.unitto.core.navigation.TOOL_ORDER_KEY
import com.sadellie.unitto.core.navigation.TopLevelRoute
import com.sadellie.unitto.core.navigation.additionalDrawerItems
import com.sadellie.unitto.core.navigation.inDefaultOrder
import com.sadellie.unitto.core.navigation.mainDrawerItems
import com.sadellie.unitto.core.navigation.ordered
import com.sadellie.unitto.core.ui.BackHandler
import com.sadellie.unitto.core.ui.LocalKeyAnimation
import com.sadellie.unitto.core.ui.LocalPhoneKeypad
import com.sadellie.unitto.core.ui.LocalPlatform
import com.sadellie.unitto.core.ui.NavigationDrawer
import com.sadellie.unitto.core.ui.Platform
import com.sadellie.unitto.feature.settings.navigation.LocalThemmoController
import com.sadellie.unitto.feature.settings.navigation.navigateToUnitGroups
import io.github.sadellie.themmo.ThemmoController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

@Composable
fun MainAppContent(
  backStack: NavBackStack<NavKey>,
  onDrawerItemClick: (DrawerItem) -> Unit,
  themmoController: ThemmoController,
) {
  val drawerScope = rememberCoroutineScope()
  val drawerState = rememberDrawerState(DrawerValue.Closed)

  val currentRoute = backStack.lastOrNull()
  val currentTopLevelRoute = remember(currentRoute) { backStack.currentTopLevelRoute() }
  val userPrefsRepository = koinInject<UserPreferencesRepository>()
  val savedLayout by
    remember(userPrefsRepository) { userPrefsRepository.appData(DRAWER_LAYOUT_KEY) }
      .collectAsState(initial = "")
  val savedOrder by
    remember(userPrefsRepository) { userPrefsRepository.appData(TOOL_ORDER_KEY) }
      .collectAsState(initial = "")
  // The web version has no home grid, all of its tools are in the menu
  val hasHome = LocalPlatform.current != Platform.Wasm
  val drawerItems =
    if (hasHome) {
      DrawerLayout.parse(savedLayout)
        .shown
        .map { NamedTool(it, stringResource(it.name)) }
        .inDefaultOrder()
        .ordered(savedOrder)
        .map { it.item }
    } else {
      mainDrawerItems.map { NamedTool(it, "") }.ordered(savedOrder).map { it.item }
    }
  val phoneKeypad by
    remember(userPrefsRepository) { userPrefsRepository.appData(PrefKeys.PHONE_KEYPAD_PREF_KEY) }
      .collectAsState(initial = "")

  val keyAnimation by
    remember(userPrefsRepository) { userPrefsRepository.appData(PrefKeys.KEY_ANIMATION_PREF_KEY) }
      .collectAsState(initial = "")

  CompositionLocalProvider(
    LocalPhoneKeypad provides (phoneKeypad == "true"),
    LocalKeyAnimation provides (keyAnimation != "false"),
    // A static cursor is less distracting than a blinking one
    LocalCursorBlinkEnabled provides false,
  ) {
    NavigationDrawer(
      modifier = Modifier,
      state = drawerState,
      // Phones go through the grid of tools, the menu is at the side of wide screens
      edgeGesture = false,
      mainTabs = if (hasHome) listOf(HomeDrawerItem) + drawerItems else drawerItems,
      additionalTabs = additionalDrawerItems,
      currentDestination = currentTopLevelRoute,
      onItemClick = { destination ->
        drawerScope.launch { drawerState.close() }
        navigateToTab(destination, backStack)
        onDrawerItemClick(destination)
      },
      onReorder =
        if (hasHome) {
          null
        } else {
          { tabs ->
            val order = tabs.joinToString(ORDER_SEPARATOR) { it.topLevelRoute.routeId }
            drawerScope.launch { userPrefsRepository.updateAppData(TOOL_ORDER_KEY, order) }
          }
        },
    ) {
      UnittoNavigation(
        backStack = backStack,
        themmoController = themmoController,
        drawerState = drawerState,
        hasHome = hasHome,
        onOpenTool = { tool ->
          navigateToTab(tool, backStack)
          onDrawerItemClick(tool)
        },
      )
    }
  }
}

@OptIn(KoinExperimentalAPI::class)
@Composable
private fun UnittoNavigation(
  backStack: NavBackStack<NavKey>,
  themmoController: ThemmoController,
  drawerState: DrawerState,
  hasHome: Boolean,
  onOpenTool: (DrawerItem) -> Unit,
) {
  val drawerScope = rememberCoroutineScope()
  val navigator =
    remember(backStack, drawerState, hasHome) {
      Navigator(
        backStack = backStack,
        openDrawerAction = {
          if (hasHome) {
            backStack.add(HomeStartRoute)
            while (backStack.size > 1) backStack.removeAt(0)
          } else {
            drawerScope.launch { drawerState.open() }
          }
        },
        navigateToUnitGroupsAction = backStack::navigateToUnitGroups,
        openToolAction = onOpenTool,
      )
    }
  CompositionLocalProvider(
    LocalNavigator provides navigator,
    LocalThemmoController provides themmoController,
  ) {
    NavDisplay(
      modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
      backStack = backStack,
      // A tool grows out of the grid, and shrinks back into it
      popTransitionSpec = { unittoFadeIn() togetherWith (unittoFadeOut() + scaleOut(targetScale = SCREEN_SCALE)) },
      transitionSpec = { (unittoFadeIn() + scaleIn(initialScale = SCREEN_SCALE)) togetherWith unittoFadeOut() },
      predictivePopTransitionSpec = { unittoFadeIn() togetherWith (unittoFadeOut() + scaleOut(targetScale = SCREEN_SCALE)) },
      entryDecorators =
        listOf(
          rememberSaveableStateHolderNavEntryDecorator(),
          rememberViewModelStoreNavEntryDecorator(),
          rememberDrawerCloseGestureNavEntryDecorator(drawerState, drawerScope),
        ),
      entryProvider = koinEntryProvider(),
    )
  }
}

private fun NavBackStack<NavKey>.currentTopLevelRoute(): TopLevelRoute? =
  this.lastOrNull { it is TopLevelRoute } as? TopLevelRoute

@Composable
private fun <T : Any> rememberDrawerCloseGestureNavEntryDecorator(
  drawerState: DrawerState,
  coroutineScope: CoroutineScope,
): DrawerCloseGestureNavEntryDecorator<T> =
  remember(drawerState, coroutineScope) {
    DrawerCloseGestureNavEntryDecorator(drawerState, coroutineScope)
  }

private class DrawerCloseGestureNavEntryDecorator<T : Any>(
  drawerState: DrawerState,
  coroutineScope: CoroutineScope,
) :
  NavEntryDecorator<T>(
    decorate = { entry ->
      entry.Content()
      // must be composed after Content to override any BackHandler that was composed before it
      BackHandler(drawerState.isOpen) { coroutineScope.launch { drawerState.close() } }
    }
  )

private const val SCREEN_SCALE = 0.92f
