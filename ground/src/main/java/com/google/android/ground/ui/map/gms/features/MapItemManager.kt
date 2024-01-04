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

class MapItemManager<T : Geometry, U : Any>
constructor(private val mapItemRenderer: MapItemRenderer<T, U>) {
  private val itemsByTag = mutableMapOf<Feature.Tag, U>()

  fun set(map: GoogleMap, tag: Feature.Tag, geometry: T, style: Feature.Style) {
    itemsByTag[tag] = mapItemRenderer.addMapItem(map, tag, geometry, style)
  }

  fun remove(tag: Feature.Tag) {
    itemsByTag.remove(tag)
  }
}
