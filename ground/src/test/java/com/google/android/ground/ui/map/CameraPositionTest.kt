/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.map

import com.google.common.truth.Truth.assertThat
import com.sharedtest.FakeData
import org.junit.Test

class CameraPositionTest {

  @Test
  fun serialize_deserialize_when_only_target_is_available() {
    serializeAndDeserialize(CameraPosition(target = TARGET))
  }

  @Test
  fun serialize_deserialize_when_target_and_zoomLevel_are_available() {
    serializeAndDeserialize(CameraPosition(target = TARGET, zoomLevel = ZOOM_LEVEL))
  }

  @Test
  fun serialize_deserialize_when_target_and_isAllowZoomOut_are_available() {
    serializeAndDeserialize(CameraPosition(target = TARGET, isAllowZoomOut = true))
  }

  @Test
  fun serialize_deserialize_when_target_and_bounds_are_available() {
    serializeAndDeserialize(CameraPosition(target = TARGET, bounds = BOUNDS))
  }

  @Test
  fun serialize_deserialize_when_all_fields_present() {
    serializeAndDeserialize(CameraPosition(TARGET, ZOOM_LEVEL, true, BOUNDS))
  }

  private fun serializeAndDeserialize(cameraPosition: CameraPosition) {
    val serializedValue = cameraPosition.serialize()
    val deserializedValue = CameraPosition.deserialize(serializedValue)
    assertThat(deserializedValue).isEqualTo(cameraPosition)
  }

  companion object {
    private val BOUNDS = Bounds(-10.0, -20.0, 10.0, 20.0)
    private val TARGET = FakeData.COORDINATES
    private const val ZOOM_LEVEL = 13.0f
  }
}
