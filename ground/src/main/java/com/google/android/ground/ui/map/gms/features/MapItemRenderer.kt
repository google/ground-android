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
package com.google.android.ground.ui.map.gms.features

import com.google.android.gms.maps.GoogleMap
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.ui.map.Feature

interface MapItemRenderer<T : Geometry, U> {
  fun addMapItem(
    map: GoogleMap,
    featureTag: Feature.Tag,
    geometry: T,
    style: Feature.Style,
    visible: Boolean
  ): U

  fun show(mapItem: U)

  fun hide(mapItem: U)

  fun remove(mapItem: U)
}
