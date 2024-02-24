package com.google.android.ground.persistence.remote.firebase

import com.google.firebase.firestore.GeoPoint
import com.google.ground.schema.PointData
import com.google.ground.schema.PolygonData
import com.google.ground.shared.LoiDocument
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

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
        geometry = PointData(coordinates = xy(1234.5678, 9876.5432))
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
          PolygonData(
            coordinates = listOf(listOf(xy(0.0, 0.0), xy(1.0, 0.0), xy(1.0, 1.0), xy(0.0, 0.0)))
          )
      )
    val actual = ObjectMapper().toObject(json.toMap(), LoiDocument::class)
    assertEquals(expected, actual)
  }
}

internal object MapTypeToken : TypeToken<Map<String, Any>>()

internal fun String.toMap(): Map<String, Any> = Gson().fromJson(this, MapTypeToken.type)

internal fun xy(x: Double, y: Double) = GeoPoint(y, x)
