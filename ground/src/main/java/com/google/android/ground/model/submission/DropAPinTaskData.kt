/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.model.submission

import com.google.android.ground.model.geometry.Point
import com.google.android.ground.ui.map.CameraPosition
import java8.util.Optional

data class DropAPinTaskData(val cameraPosition: CameraPosition) : TaskData {
  override fun getDetailsText(): String = cameraPosition.serialize()

  override fun isEmpty(): Boolean = false

  fun getPoint(): Point = Point(cameraPosition.target)

  companion object {
    fun fromString(serializedValue: String): Optional<TaskData> {
      val cameraPosition = CameraPosition.deserialize(serializedValue)
      return if (cameraPosition == null) Optional.empty()
      else Optional.of(DropAPinTaskData(cameraPosition))
    }
  }
}
