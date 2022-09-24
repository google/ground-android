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

import com.google.android.ground.model.geometry.Point
import com.google.android.ground.ui.home.mapcontainer.MapContainerViewModel
import java8.util.Optional

data class CameraUpdate(
  val center: Point,
  val zoomLevel: Optional<Float>,
  val isAllowZoomOut: Boolean
) {

  override fun toString(): String =
    if (zoomLevel.isPresent) {
      "Pan + zoom"
    } else {
      "Pan"
    }

  companion object {
    fun pan(center: Point): CameraUpdate = CameraUpdate(center, Optional.empty(), false)

    fun panAndZoomIn(center: Point): CameraUpdate =
      CameraUpdate(center, Optional.of(MapContainerViewModel.DEFAULT_LOI_ZOOM_LEVEL), false)

    fun panAndZoom(cameraPosition: CameraPosition): CameraUpdate =
      CameraUpdate(cameraPosition.target, Optional.of(cameraPosition.zoomLevel), true)
  }
}
