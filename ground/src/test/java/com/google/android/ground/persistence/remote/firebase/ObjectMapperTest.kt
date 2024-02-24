package com.google.android.ground.persistence.remote.firebase

import com.google.firebase.firestore.GeoPoint
import com.google.ground.shared.schema.LinearRingObject
import com.google.ground.shared.schema.LoiDocument
import com.google.ground.shared.schema.PointObject
import com.google.ground.shared.schema.PolygonObject
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
            "coordinates": {"latitude": 12.34, "longitude": 56.78}
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
          PointObject(coordinates = GeoPoint(/* latitude= */ 12.34, /* longitude= */ 56.78))
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
              {
                "$": [
                  {"latitude": 0.0, "longitude": 0.0},
                  {"latitude": 0.0, "longitude": 1.0},
                  {"latitude": 1.0, "longitude": 1.0},              
                  {"latitude": 0.0, "longitude": 0.0}
                ]
              }
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
          PolygonObject(
            coordinates =
              listOf(
                LinearRingObject(
                  listOf(
                    GeoPoint(/* latitude= */ 0.0, /* longitude= */ 0.0),
                    GeoPoint(/* latitude= */ 0.0, /* longitude= */ 1.0),
                    GeoPoint(/* latitude= */ 1.0, /* longitude= */ 1.0),
                    GeoPoint(/* latitude= */ 0.0, /* longitude= */ 0.0)
                  )
                )
              )
          )
      )
    val actual = ObjectMapper().toObject(json.toMap(), LoiDocument::class)
    assertEquals(expected, actual)
  }
}

internal object MapTypeToken : TypeToken<Map<String, Any>>()

internal fun String.toMap(): Map<String, Any> = Gson().fromJson(this, MapTypeToken.type)
