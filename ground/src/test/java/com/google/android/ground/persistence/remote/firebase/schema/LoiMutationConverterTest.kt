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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.mutation.Mutation
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.GeoPoint
import com.sharedtest.FakeData
import kotlin.test.fail
import org.junit.Assert.assertThrows
import org.junit.Test

class LoiMutationConverterTest {
  @Test
  fun `toMap() retains job ID and submission count`() {
    with(LoiConverter) {
      val mutation = FakeData.newLoiMutation(TEST_POINT)
      val map = LoiMutationConverter.toMap(mutation, TEST_USER)
      assertThat(map[JOB_ID]).isEqualTo(mutation.jobId)
      assertThat(map[SUBMISSION_COUNT]).isEqualTo(mutation.submissionCount)
    }
  }

  @Test
  fun `toMap() retains point geometry data`() {
    with(LoiConverter) {
      val mutation = FakeData.newLoiMutation(TEST_POINT)
      val map = LoiMutationConverter.toMap(mutation, TEST_USER)
      val geometry = map[GEOMETRY]
      if (geometry is MutableMap<*, *>) {
        assertThat(geometry[GEOMETRY_TYPE]).isEqualTo(POINT_TYPE)
        assertThat(geometry[GEOMETRY_COORDINATES]).isEqualTo(GeoPoint(88.0, -23.1))
      } else {
        fail("GEOMETRY field, $geometry, is not a map.")
      }
    }
  }

  @Test
  fun `toMap() retains polygon geometry data`() {
    with(LoiConverter) {
      val mutation = FakeData.newAoiMutation(TEST_POLYGON)
      val map = LoiMutationConverter.toMap(mutation, TEST_USER)
      val geometry = map[GEOMETRY]
      if (geometry is MutableMap<*, *>) {
        assertThat(geometry[GEOMETRY_TYPE]).isEqualTo(POLYGON_TYPE)
        assertThat((geometry[GEOMETRY_COORDINATES] as MutableMap<*, *>).values.size).isEqualTo(1)
        val coordinates =
          ((geometry[GEOMETRY_COORDINATES] as MutableMap<*, *>)["0"] as MutableMap<*, *>).toList()
        assertThat(coordinates.size).isEqualTo(3)
        assertThat(coordinates[0]).isEqualTo("0" to GeoPoint(0.0, 1.0))
        assertThat(coordinates[1]).isEqualTo("1" to GeoPoint(1.0, 1.0))
        assertThat(coordinates[2]).isEqualTo("2" to GeoPoint(0.0, 1.0))
      } else {
        fail("GEOMETRY field, $geometry, is not a map.")
      }
    }
  }

  @Test
  fun `toMap() converts CREATE mutation to map`() {
    with(LoiConverter) {
      val mutation = FakeData.newLoiMutation(TEST_POINT, mutationType = Mutation.Type.CREATE)
      val map = LoiMutationConverter.toMap(mutation, TEST_USER)
      assertThat(map[CREATED]).isEqualTo(map[LAST_MODIFIED])
      assertThat(map[CREATED])
        .isEqualTo(AuditInfoConverter.fromMutationAndUser(mutation, TEST_USER))
      assertThat(map[IS_PLANNED]).isEqualTo(false)
    }
  }

  @Test
  fun `toMap() converts UPDATE mutation to map`() {
    with(LoiConverter) {
      val mutation = FakeData.newLoiMutation(TEST_POINT, mutationType = Mutation.Type.UPDATE)
      val map = LoiMutationConverter.toMap(mutation, TEST_USER)
      assertThat(map[CREATED]).isNotEqualTo(map[LAST_MODIFIED])
      assertThat(map[LAST_MODIFIED])
        .isEqualTo(AuditInfoConverter.fromMutationAndUser(mutation, TEST_USER))
    }
  }

  @Test
  fun `toMap() throws an error for DELETE and UNKOWN mutation`() {
    val deleteMutation = FakeData.newLoiMutation(TEST_POINT, mutationType = Mutation.Type.DELETE)
    assertThrows(UnsupportedOperationException::class.java) {
      LoiMutationConverter.toMap(deleteMutation, TEST_USER)
    }
    val unknownMutation = FakeData.newLoiMutation(TEST_POINT, mutationType = Mutation.Type.UNKNOWN)
    assertThrows(UnsupportedOperationException::class.java) {
      LoiMutationConverter.toMap(unknownMutation, TEST_USER)
    }
  }

  companion object {
    private val TEST_USER = FakeData.USER
    private val TEST_POINT = Point(Coordinates(88.0, -23.1))
    private val TEST_POLYGON =
      listOf(Coordinates(0.0, 1.0), Coordinates(1.0, 1.0), Coordinates(0.0, 1.0))
  }
}
