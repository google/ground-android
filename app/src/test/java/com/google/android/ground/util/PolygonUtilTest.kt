package com.google.android.ground.util

import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.ui.util.calculateShoelacePolygonArea
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PolygonUtilTest {

  @Test
  fun `calculateShoelacePolygonArea should return correct area for simple square`() {
    val coordinates =
      listOf(
        Coordinates(0.0, 0.0),
        Coordinates(0.0, 0.00001),
        Coordinates(0.00001, 0.00001),
        Coordinates(0.00001, 0.0),
        Coordinates(0.0, 0.0), // Closing the polygon
      )

    val area = calculateShoelacePolygonArea(coordinates)
    assertEquals(1.24, area, 0.01) // Allowing minor floating-point error
  }

  @Test
  fun `calculateShoelacePolygonArea should return 0 for less than 3 points`() {
    val coordinates = listOf(Coordinates(24.523740, 73.606673), Coordinates(24.523736, 73.606803))

    val area = calculateShoelacePolygonArea(coordinates)
    assertEquals(0.0, area, 0.01)
  }
}
