package com.google.android.ground.persistence.remote.firebase

import com.google.ground.shared.schema.geometry.Point
import com.google.ground.shared.schema.geometry.Polygon
import com.google.ground.shared.schema.loi.LoiDocument
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.math.BigDecimal
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class ObjectMapperTest {
  @Test
  fun `Converts Point LOI`() {
    val json =
      """
        {
          "jobId": "123",
          "customId": "foo",
          "submissionCount": 42,
          "geometry": {
            "type": "Point",
            "coordinates": [1234.5678, 9876.5432]
          }
        }
        """
        .trimIndent()
    val expected =
      LoiDocument(
        jobId = "123",
        customId = "foo",
        submissionCount = 42,
        geometry = Point(coordinates = xy(1234.5678, 9876.5432))
      )
    val actual = ObjectMapper().toObject(json.toMap(), LoiDocument::class)
    assertEquals(expected, actual)
  }

  @Test
  fun `Converts basic Polygon LOI`() {
    // TODO: Test Polygon with holes.
    val json =
      """
        {
          "jobId": "123",
          "customId": "foo",
          "submissionCount": 42,
          "geometry": {
            "type": "Polygon",
            "coordinates": [
              [
                [0.0, 0.0],
                [1.0, 0.0],
                [1.0, 1.0],              
                [0.0, 0.0]
              ]
            ]
          }
        }
        """
        .trimIndent()
    val expected =
      LoiDocument(
        jobId = "123",
        customId = "foo",
        submissionCount = 42,
        geometry =
          Polygon(
            coordinates = listOf(listOf(xy(0.0, 0.0), xy(1.0, 0.0), xy(1.0, 1.0), xy(0.0, 0.0)))
          )
      )
    val actual = ObjectMapper().toObject(json.toMap(), LoiDocument::class)
    assertEquals(expected, actual)
  }
}

internal object MapTypeToken : TypeToken<Map<String, Any>>()

internal fun String.toMap(): Map<String, Any> = Gson().fromJson(this, MapTypeToken.type)

internal fun xy(x: Double, y: Double): List<BigDecimal> = listOf(x.toBigDecimal(), y.toBigDecimal())
