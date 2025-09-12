/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.groundplatform.android.data.remote.firebase.protobuf

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.util.Date
import org.groundplatform.android.FakeData
import org.groundplatform.android.FakeData.LOCATION_OF_INTEREST_NAME
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.LinearRing
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.model.locationofinterest.LOI_NAME_PROPERTY
import org.groundplatform.android.model.mutation.LocationOfInterestMutation
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.proto.AuditInfo.CLIENT_TIMESTAMP_FIELD_NUMBER
import org.groundplatform.android.proto.AuditInfo.DISPLAY_NAME_FIELD_NUMBER
import org.groundplatform.android.proto.AuditInfo.SERVER_TIMESTAMP_FIELD_NUMBER
import org.groundplatform.android.proto.AuditInfo.USER_ID_FIELD_NUMBER
import org.groundplatform.android.proto.Coordinates.LATITUDE_FIELD_NUMBER
import org.groundplatform.android.proto.Coordinates.LONGITUDE_FIELD_NUMBER
import org.groundplatform.android.proto.Geometry.POINT_FIELD_NUMBER
import org.groundplatform.android.proto.Geometry.POLYGON_FIELD_NUMBER
import org.groundplatform.android.proto.LocationOfInterest.CREATED_FIELD_NUMBER
import org.groundplatform.android.proto.LocationOfInterest.CUSTOM_TAG_FIELD_NUMBER
import org.groundplatform.android.proto.LocationOfInterest.GEOMETRY_FIELD_NUMBER
import org.groundplatform.android.proto.LocationOfInterest.JOB_ID_FIELD_NUMBER
import org.groundplatform.android.proto.LocationOfInterest.LAST_MODIFIED_FIELD_NUMBER
import org.groundplatform.android.proto.LocationOfInterest.PROPERTIES_FIELD_NUMBER
import org.groundplatform.android.proto.LocationOfInterest.Property.STRING_VALUE_FIELD_NUMBER
import org.groundplatform.android.proto.LocationOfInterest.SOURCE_FIELD_NUMBER
import org.groundplatform.android.proto.LocationOfInterest.SUBMISSION_COUNT_FIELD_NUMBER
import org.groundplatform.android.proto.Point.COORDINATES_FIELD_NUMBER
import org.groundplatform.android.proto.Polygon.SHELL_FIELD_NUMBER
import org.junit.Assert.assertThrows
import org.junit.Test

class LoiMutationConverterTest {
  @Test
  fun `toMap() retains job ID and submission count`() {
    val mutation = newLoiMutation()

    val map = mutation.createLoiMessage(TEST_USER).toFirestoreMap()

    assertThat(map[JOB_ID_FIELD_NUMBER.toString()]).isEqualTo(mutation.jobId)
    assertThat(map[SUBMISSION_COUNT_FIELD_NUMBER.toString()]).isEqualTo(10)
  }

  @Test
  fun `toMap() retains point geometry data`() {
    val mutation = newLoiMutation()

    val map = mutation.createLoiMessage(TEST_USER).toFirestoreMap()

    val geometry = map[GEOMETRY_FIELD_NUMBER.toString()] as MutableMap<*, *>
    assertThat(geometry[POINT_FIELD_NUMBER.toString()])
      .isEqualTo(
        mapOf(
          COORDINATES_FIELD_NUMBER.toString() to
            mapOf(
              LATITUDE_FIELD_NUMBER.toString() to 88.0,
              LONGITUDE_FIELD_NUMBER.toString() to -23.1,
            )
        )
      )
  }

  @Test
  fun `toMap() retains polygon geometry data`() {
    val mutation = newAoiMutation()

    val map = mutation.createLoiMessage(TEST_USER).toFirestoreMap()

    val geometry = map[GEOMETRY_FIELD_NUMBER.toString()] as MutableMap<*, *>
    assertThat(geometry[POLYGON_FIELD_NUMBER.toString()])
      .isEqualTo(
        mapOf(
          SHELL_FIELD_NUMBER.toString() to
            mapOf(
              COORDINATES_FIELD_NUMBER.toString() to
                listOf(
                  mapOf(LONGITUDE_FIELD_NUMBER.toString() to 1.0),
                  mapOf(
                    LATITUDE_FIELD_NUMBER.toString() to 1.0,
                    LONGITUDE_FIELD_NUMBER.toString() to 1.0,
                  ),
                  mapOf(LONGITUDE_FIELD_NUMBER.toString() to 1.0),
                )
            )
        )
      )
  }

  @Test
  fun `toMap() retains properties`() {
    val mutation = newLoiMutation()

    val map = mutation.createLoiMessage(TEST_USER).toFirestoreMap()

    assertThat(map[PROPERTIES_FIELD_NUMBER.toString()])
      .isEqualTo(
        mapOf<String, Any>(
          LOI_NAME_PROPERTY to
            mapOf<String, Any>(STRING_VALUE_FIELD_NUMBER.toString() to LOCATION_OF_INTEREST_NAME)
        )
      )
  }

  @Test
  fun `toMap() retains customTag`() {
    val mutation = newLoiMutation()

    val map = mutation.createLoiMessage(TEST_USER).toFirestoreMap()
    assertThat(map[CUSTOM_TAG_FIELD_NUMBER.toString()]).isEqualTo(mutation.customId)
  }

  @Test
  fun `toMap() customTag is empty when empty`() {
    // NOTE(#2626): Was previously erroneously set to the property name.
    val mutation =
      LocationOfInterestMutation(
        userId = TEST_USER.id,
        type = Mutation.Type.CREATE,
        properties = mapOf(LOI_NAME_PROPERTY to LOCATION_OF_INTEREST_NAME),
        collectionId = "collectionId",
      )

    val map = mutation.createLoiMessage(TEST_USER).toFirestoreMap()
    assertThat(map[CUSTOM_TAG_FIELD_NUMBER.toString()]).isNull()
  }

  @Test
  fun `toMap() converts CREATE mutation to map`() {
    val mutation = newLoiMutation(mutationType = Mutation.Type.CREATE)

    val map = mutation.createLoiMessage(TEST_USER).toFirestoreMap()

    assertThat(map[CREATED_FIELD_NUMBER.toString()])
      .isEqualTo(
        mapOf(
          USER_ID_FIELD_NUMBER.toString() to TEST_USER.id,
          DISPLAY_NAME_FIELD_NUMBER.toString() to TEST_USER.displayName,
          CLIENT_TIMESTAMP_FIELD_NUMBER.toString() to mapOf("1" to 987654321L),
          SERVER_TIMESTAMP_FIELD_NUMBER.toString() to mapOf("1" to 987654321L),
        )
      )
    assertThat(map[CREATED_FIELD_NUMBER.toString()])
      .isEqualTo(map[LAST_MODIFIED_FIELD_NUMBER.toString()])
    assertThat(map[SOURCE_FIELD_NUMBER.toString()]).isNull()
  }

  @Test
  fun `toMap() converts UPDATE mutation to map`() {
    val mutation = newLoiMutation(mutationType = Mutation.Type.UPDATE)

    val map = mutation.createLoiMessage(TEST_USER).toFirestoreMap()

    assertThat(map[CREATED_FIELD_NUMBER.toString()])
      .isNotEqualTo(map[LAST_MODIFIED_FIELD_NUMBER.toString()])
    assertThat(map[LAST_MODIFIED_FIELD_NUMBER.toString()])
      .isEqualTo(
        mapOf(
          USER_ID_FIELD_NUMBER.toString() to TEST_USER.id,
          DISPLAY_NAME_FIELD_NUMBER.toString() to TEST_USER.displayName,
          CLIENT_TIMESTAMP_FIELD_NUMBER.toString() to mapOf("1" to 987654321L),
          SERVER_TIMESTAMP_FIELD_NUMBER.toString() to mapOf("1" to 987654321L),
        )
      )
  }

  @Test
  fun `toMap() throws an error for DELETE and UNKOWN mutation`() {
    val deleteMutation = newLoiMutation(mutationType = Mutation.Type.DELETE)
    assertThrows(UnsupportedOperationException::class.java) {
      deleteMutation.createLoiMessage(TEST_USER).toFirestoreMap()
    }
    val unknownMutation = newLoiMutation(mutationType = Mutation.Type.UNKNOWN)
    assertThrows(UnsupportedOperationException::class.java) {
      unknownMutation.createLoiMessage(TEST_USER).toFirestoreMap()
    }
  }

  companion object {
    private val TEST_USER = FakeData.USER
    private val TEST_POINT = Point(Coordinates(88.0, -23.1))
    private val TEST_POLYGON =
      Polygon(
        LinearRing(listOf(Coordinates(0.0, 1.0), Coordinates(1.0, 1.0), Coordinates(0.0, 1.0)))
      )

    fun newLoiMutation(
      point: Point = TEST_POINT,
      mutationType: Mutation.Type = Mutation.Type.CREATE,
      syncStatus: Mutation.SyncStatus = Mutation.SyncStatus.PENDING,
    ) =
      LocationOfInterestMutation(
        jobId = "jobId",
        geometry = point,
        id = 1L,
        locationOfInterestId = "loiId",
        type = mutationType,
        syncStatus = syncStatus,
        userId = TEST_USER.id,
        surveyId = "surveyId",
        clientTimestamp = Date.from(Instant.ofEpochSecond(987654321)),
        submissionCount = 10,
        properties = mapOf(LOI_NAME_PROPERTY to LOCATION_OF_INTEREST_NAME),
        customId = "a custom loi",
        collectionId = "collectionId",
      )

    fun newAoiMutation(
      polygon: Polygon = TEST_POLYGON,
      mutationType: Mutation.Type = Mutation.Type.CREATE,
      syncStatus: Mutation.SyncStatus = Mutation.SyncStatus.PENDING,
    ) =
      LocationOfInterestMutation(
        jobId = "jobId",
        geometry = polygon,
        id = 1L,
        locationOfInterestId = "loiId",
        type = mutationType,
        syncStatus = syncStatus,
        userId = TEST_USER.id,
        surveyId = "surveyId",
        clientTimestamp = Date.from(Instant.ofEpochSecond(987654321)),
        properties = mapOf(LOI_NAME_PROPERTY to LOCATION_OF_INTEREST_NAME),
        customId = "a custom loi",
        collectionId = "collectionId",
      )
  }
}
