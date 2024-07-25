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
import com.google.android.ground.proto.LocationOfInterest
import com.google.android.ground.proto.LocationOfInterestKt.property
import com.google.android.ground.proto.auditInfo
import com.google.android.ground.proto.coordinates
import com.google.android.ground.proto.geometry
import com.google.android.ground.proto.locationOfInterest
import com.google.android.ground.proto.point
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.timestamp
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
        clientTimestamp = Date.from(Instant.ofEpochSecond(987654321)),
        geometry = null,
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = false,
      )

    val output = mutation.createLoiMessage(user)

    assertThat(output)
      .isEqualTo(
        locationOfInterest {
          id = "loiId"
          jobId = "jobId"
          submissionCount = 1
          ownerId = "userId"
          created = auditInfo {
            userId = "userId"
            displayName = "User"
            photoUrl = ""
            clientTimestamp = timestamp { seconds = 987654321L }
            serverTimestamp = timestamp { seconds = 987654321L }
          }
          lastModified = auditInfo {
            userId = "userId"
            displayName = "User"
            photoUrl = ""
            clientTimestamp = timestamp { seconds = 987654321L }
            serverTimestamp = timestamp { seconds = 987654321L }
          }
          customTag = "customId"
          source = LocationOfInterest.Source.FIELD_DATA
          properties.putAll(mapOf("name" to property { stringValue = "loiName" }))
        }
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
        clientTimestamp = Date.from(Instant.ofEpochSecond(987654321)),
        geometry = Point(Coordinates(10.0, 20.0)),
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = null,
      )

    val output = mutation.createLoiMessage(user)

    assertThat(output)
      .isEqualTo(
        locationOfInterest {
          id = "loiId"
          jobId = "jobId"
          submissionCount = 1
          ownerId = "userId"
          created = auditInfo {
            userId = "userId"
            displayName = "User"
            photoUrl = ""
            clientTimestamp = timestamp { seconds = 987654321L }
            serverTimestamp = timestamp { seconds = 987654321L }
          }
          lastModified = auditInfo {
            userId = "userId"
            displayName = "User"
            photoUrl = ""
            clientTimestamp = timestamp { seconds = 987654321L }
            serverTimestamp = timestamp { seconds = 987654321L }
          }
          customTag = "customId"
          geometry = geometry {
            point = point {
              coordinates = coordinates {
                latitude = 10.0
                longitude = 20.0
              }
            }
          }
          properties.putAll(mapOf("name" to property { stringValue = "loiName" }))
        }
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
        clientTimestamp = Date.from(Instant.ofEpochSecond(987654321)),
        geometry = Point(Coordinates(10.0, 20.0)),
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = true,
      )

    val output = mutation.createLoiMessage(user)

    assertThat(output)
      .isEqualTo(
        locationOfInterest {
          id = "loiId"
          jobId = "jobId"
          submissionCount = 1
          ownerId = "userId"
          created = auditInfo {
            userId = "userId"
            displayName = "User"
            photoUrl = ""
            clientTimestamp = timestamp { seconds = 987654321L }
            serverTimestamp = timestamp { seconds = 987654321L }
          }
          lastModified = auditInfo {
            userId = "userId"
            displayName = "User"
            photoUrl = ""
            clientTimestamp = timestamp { seconds = 987654321L }
            serverTimestamp = timestamp { seconds = 987654321L }
          }
          customTag = "customId"
          geometry = geometry {
            point = point {
              coordinates = coordinates {
                latitude = 10.0
                longitude = 20.0
              }
            }
          }
          source = LocationOfInterest.Source.IMPORTED
          properties.putAll(mapOf("name" to property { stringValue = "loiName" }))
        }
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
        clientTimestamp = Date.from(Instant.ofEpochSecond(987654321)),
        geometry = Point(Coordinates(10.0, 20.0)),
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = false,
      )

    val output = mutation.createLoiMessage(user)

    assertThat(output)
      .isEqualTo(
        locationOfInterest {
          id = "loiId"
          jobId = "jobId"
          submissionCount = 1
          ownerId = "userId"
          created = auditInfo {
            userId = "userId"
            displayName = "User"
            photoUrl = ""
            clientTimestamp = timestamp { seconds = 987654321L }
            serverTimestamp = timestamp { seconds = 987654321L }
          }
          lastModified = auditInfo {
            userId = "userId"
            displayName = "User"
            photoUrl = ""
            clientTimestamp = timestamp { seconds = 987654321L }
            serverTimestamp = timestamp { seconds = 987654321L }
          }
          customTag = "customId"
          geometry = geometry {
            point = point {
              coordinates = coordinates {
                latitude = 10.0
                longitude = 20.0
              }
            }
          }
          source = LocationOfInterest.Source.FIELD_DATA
          properties.putAll(mapOf("name" to property { stringValue = "loiName" }))
        }
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
        clientTimestamp = Date.from(Instant.ofEpochSecond(987654321)),
        geometry = Point(Coordinates(10.0, 20.0)),
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = false,
      )

    val output = mutation.createLoiMessage(user)

    assertThat(output)
      .isEqualTo(
        locationOfInterest {
          id = "loiId"
          jobId = "jobId"
          submissionCount = 1
          ownerId = "userId"
          lastModified = auditInfo {
            userId = "userId"
            displayName = "User"
            photoUrl = ""
            clientTimestamp = timestamp { seconds = 987654321L }
            serverTimestamp = timestamp { seconds = 987654321L }
          }
          customTag = "customId"
          geometry = geometry {
            point = point {
              coordinates = coordinates {
                latitude = 10.0
                longitude = 20.0
              }
            }
          }
          properties.putAll(mapOf("name" to property { stringValue = "loiName" }))
        }
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
        clientTimestamp = Date.from(Instant.ofEpochSecond(987654321)),
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
        clientTimestamp = Date.from(Instant.ofEpochSecond(987654321)),
        geometry = Point(Coordinates(10.0, 20.0)),
        submissionCount = 1,
        properties = generateProperties("loiName"),
        isPredefined = false,
      )

    assertThrows(UnsupportedOperationException::class.java) { mutation.createLoiMessage(user) }
  }
}
