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
import org.junit.Test

class ModelToProtoExtKtTest {

  @Test
  fun test() {
    val user = User("userId", "user@gmail.com", "User")
    val mutation =
      LocationOfInterestMutation(
        type = Mutation.Type.CREATE,
        syncStatus = Mutation.SyncStatus.PENDING,
        surveyId = "surveyId",
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
          "3" to mapOf<String, Any>(),
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
          "10" to mapOf("name" to mapOf<String, Any>()),
        )
      )
  }
}
