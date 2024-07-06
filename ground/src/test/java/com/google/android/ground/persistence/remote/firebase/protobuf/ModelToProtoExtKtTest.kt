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
package com.google.android.ground.persistence.remote.firebase.protobuf

import com.google.android.ground.model.User
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.locationofinterest.generateProperties
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.util.Date
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelToProtoExtKtTest {

  @Test
  fun `createLoiMessage() when geometry is null`() {
    val user = User("userId", "user@gmail.com", "User")
    val mutation =
      LocationOfInterestMutation(
        type = Mutation.Type.CREATE,
        syncStatus = Mutation.SyncStatus.PENDING, // this field is ignored
        surveyId = "surveyId", // this field is ignored
        locationOfInterestId = "loiId",
        userId = "userId",
        jobId = "jobId",
        customId = "customId",
        clientTimestamp = Date.from(Instant.ofEpochMilli(1000)),
        geometry = null,
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = false,
      )
    val proto = mutation.createLoiMessage(user)
    val output = proto.toFirestoreMap()

    assertThat(output)
      .isEqualTo(
        mapOf(
          "1" to "loiId",
          "2" to "jobId",
          "4" to 1,
          "5" to "userId",
          "6" to
            mapOf(
              "1" to "userId",
              "2" to mapOf("1" to 1000000L),
              "3" to mapOf("1" to 1000000L),
              "4" to "User",
            ),
          "7" to
            mapOf(
              "1" to "userId",
              "2" to mapOf("1" to 1000000L),
              "3" to mapOf("1" to 1000000L),
              "4" to "User",
            ),
          "8" to "customId",
          "9" to 1,
          "10" to mapOf("name" to mapOf("1" to "loiName")),
        )
      )
  }

  @Test
  fun `createLoiMessage() when isPredefined is null and type is CREATE`() {
    val user = User("userId", "user@gmail.com", "User")
    val mutation =
      LocationOfInterestMutation(
        type = Mutation.Type.CREATE,
        syncStatus = Mutation.SyncStatus.PENDING, // this field is ignored
        surveyId = "surveyId", // this field is ignored
        locationOfInterestId = "loiId",
        userId = "userId",
        jobId = "jobId",
        customId = "customId",
        clientTimestamp = Date.from(Instant.ofEpochMilli(1000)),
        geometry = Point(Coordinates(10.0, 20.0)),
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = null,
      )
    val proto = mutation.createLoiMessage(user)
    val output = proto.toFirestoreMap()

    assertThat(output)
      .isEqualTo(
        mapOf(
          "1" to "loiId",
          "2" to "jobId",
          "3" to mapOf("1" to mapOf("1" to mapOf("1" to 10.0, "2" to 20.0))),
          "4" to 1,
          "5" to "userId",
          "6" to
            mapOf(
              "1" to "userId",
              "2" to mapOf("1" to 1000000L),
              "3" to mapOf("1" to 1000000L),
              "4" to "User",
            ),
          "7" to
            mapOf(
              "1" to "userId",
              "2" to mapOf("1" to 1000000L),
              "3" to mapOf("1" to 1000000L),
              "4" to "User",
            ),
          "8" to "customId",
          "10" to mapOf("name" to mapOf("1" to "loiName")),
        )
      )
  }

  @Test
  fun `createLoiMessage() when isPredefined is true and type is CREATE`() {
    val user = User("userId", "user@gmail.com", "User")
    val mutation =
      LocationOfInterestMutation(
        type = Mutation.Type.CREATE,
        syncStatus = Mutation.SyncStatus.PENDING, // this field is ignored
        surveyId = "surveyId", // this field is ignored
        locationOfInterestId = "loiId",
        userId = "userId",
        jobId = "jobId",
        customId = "customId",
        clientTimestamp = Date.from(Instant.ofEpochMilli(1000)),
        geometry = Point(Coordinates(10.0, 20.0)),
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = true,
      )
    val proto = mutation.createLoiMessage(user)
    val output = proto.toFirestoreMap()

    assertThat(output)
      .isEqualTo(
        mapOf(
          "1" to "loiId",
          "2" to "jobId",
          "3" to mapOf("1" to mapOf("1" to mapOf("1" to 10.0, "2" to 20.0))),
          "4" to 1,
          "5" to "userId",
          "6" to
            mapOf(
              "1" to "userId",
              "2" to mapOf("1" to 1000000L),
              "3" to mapOf("1" to 1000000L),
              "4" to "User",
            ),
          "7" to
            mapOf(
              "1" to "userId",
              "2" to mapOf("1" to 1000000L),
              "3" to mapOf("1" to 1000000L),
              "4" to "User",
            ),
          "8" to "customId",
          "9" to 2,
          "10" to mapOf("name" to mapOf("1" to "loiName")),
        )
      )
  }

  @Test
  fun `createLoiMessage() when isPredefined is false and type is CREATE`() {
    val user = User("userId", "user@gmail.com", "User")
    val mutation =
      LocationOfInterestMutation(
        type = Mutation.Type.CREATE,
        syncStatus = Mutation.SyncStatus.PENDING, // this field is ignored
        surveyId = "surveyId", // this field is ignored
        locationOfInterestId = "loiId",
        userId = "userId",
        jobId = "jobId",
        customId = "customId",
        clientTimestamp = Date.from(Instant.ofEpochMilli(1000)),
        geometry = Point(Coordinates(10.0, 20.0)),
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = false,
      )
    val proto = mutation.createLoiMessage(user)
    val output = proto.toFirestoreMap()

    assertThat(output)
      .isEqualTo(
        mapOf(
          "1" to "loiId",
          "2" to "jobId",
          "3" to mapOf("1" to mapOf("1" to mapOf("1" to 10.0, "2" to 20.0))),
          "4" to 1,
          "5" to "userId",
          "6" to
            mapOf(
              "1" to "userId",
              "2" to mapOf("1" to 1000000L),
              "3" to mapOf("1" to 1000000L),
              "4" to "User",
            ),
          "7" to
            mapOf(
              "1" to "userId",
              "2" to mapOf("1" to 1000000L),
              "3" to mapOf("1" to 1000000L),
              "4" to "User",
            ),
          "8" to "customId",
          "9" to 1,
          "10" to mapOf("name" to mapOf("1" to "loiName")),
        )
      )
  }

  @Test
  fun `createLoiMessage() when type is UPDATE`() {
    val user = User("userId", "user@gmail.com", "User")
    val mutation =
      LocationOfInterestMutation(
        type = Mutation.Type.UPDATE,
        syncStatus = Mutation.SyncStatus.PENDING, // this field is ignored
        surveyId = "surveyId", // this field is ignored
        locationOfInterestId = "loiId",
        userId = "userId",
        jobId = "jobId",
        customId = "customId",
        clientTimestamp = Date.from(Instant.ofEpochMilli(1000)),
        geometry = Point(Coordinates(10.0, 20.0)),
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = false,
      )
    val proto = mutation.createLoiMessage(user)
    val output = proto.toFirestoreMap()

    assertThat(output)
      .isEqualTo(
        mapOf(
          "1" to "loiId",
          "2" to "jobId",
          "3" to mapOf("1" to mapOf("1" to mapOf("1" to 10.0, "2" to 20.0))),
          "4" to 1,
          "5" to "userId",
          "7" to
            mapOf(
              "1" to "userId",
              "2" to mapOf("1" to 1000000L),
              "3" to mapOf("1" to 1000000L),
              "4" to "User",
            ),
          "8" to "customId",
          "10" to mapOf("name" to mapOf("1" to "loiName")),
        )
      )
  }

  @Test
  fun `createLoiMessage() when type is DELETE throws error`() {
    val user = User("userId", "user@gmail.com", "User")
    val mutation =
      LocationOfInterestMutation(
        type = Mutation.Type.DELETE,
        syncStatus = Mutation.SyncStatus.PENDING, // this field is ignored
        surveyId = "surveyId", // this field is ignored
        locationOfInterestId = "loiId",
        userId = "userId",
        jobId = "jobId",
        customId = "customId",
        clientTimestamp = Date.from(Instant.ofEpochMilli(1000)),
        geometry = Point(Coordinates(10.0, 20.0)),
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = false,
      )

    assertThrows(UnsupportedOperationException::class.java) { mutation.createLoiMessage(user) }
  }

  @Test
  fun `createLoiMessage() when type is UNKNOWN throws error`() {
    val user = User("userId", "user@gmail.com", "User")
    val mutation =
      LocationOfInterestMutation(
        type = Mutation.Type.UNKNOWN,
        syncStatus = Mutation.SyncStatus.PENDING, // this field is ignored
        surveyId = "surveyId", // this field is ignored
        locationOfInterestId = "loiId",
        userId = "userId",
        jobId = "jobId",
        customId = "customId",
        clientTimestamp = Date.from(Instant.ofEpochMilli(1000)),
        geometry = Point(Coordinates(10.0, 20.0)),
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = false,
      )

    assertThrows(UnsupportedOperationException::class.java) { mutation.createLoiMessage(user) }
  }
}
