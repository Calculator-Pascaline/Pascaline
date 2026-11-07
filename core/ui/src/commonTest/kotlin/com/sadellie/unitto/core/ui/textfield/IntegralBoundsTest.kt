package com.sadellie.unitto.core.ui.textfield

import com.sadellie.unitto.core.common.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IntegralBoundsTest {
  private val noBounds = "1+" + Token.Calculus.INTEGRAL + "x" + Token.Calculus.DX

  @Test
  fun findsTheIntegralOnBothSidesOfItsSign() {
    assertEquals(2, noBounds.boundlessIntegralAt(2))
    assertEquals(2, noBounds.boundlessIntegralAt(3))
  }

  @Test
  fun otherPlacesAreNotAnIntegral() {
    assertNull(noBounds.boundlessIntegralAt(0))
    assertNull(noBounds.boundlessIntegralAt(4))
  }

  @Test
  fun integralWithBoundsStaysAsItIs() {
    val bounded = Token.Calculus.INTEGRAL + INTEGRAL_BOUNDS + "x" + Token.Calculus.DX
    assertNull(bounded.boundlessIntegralAt(0))
    assertNull(bounded.boundlessIntegralAt(1))
  }
}
