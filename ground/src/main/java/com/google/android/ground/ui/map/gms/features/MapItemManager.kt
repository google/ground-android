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

class MapItemManager<T : Geometry, U : Any>(private val mapItemAdapter: MapItemAdapter<T, U>) {
  private val itemsByTag = mutableMapOf<Feature.Tag, U>()

  val items: Iterable<U>
    get() = itemsByTag.values

  fun set(map: GoogleMap, tag: Feature.Tag, geometry: T, style: Feature.Style, visible: Boolean) {
    // If map item with this tag already exists, remove it.
    itemsByTag[tag]?.let(mapItemAdapter::remove)
    // Add item to map and index.
    itemsByTag[tag] = mapItemAdapter.addMapItem(map, tag, geometry, style, visible)
  }

  fun show(tag: Feature.Tag) {
    itemsByTag[tag]?.let(mapItemAdapter::show)
  }

  fun hide(tag: Feature.Tag) {
    itemsByTag[tag]?.let(mapItemAdapter::hide)
  }

  fun remove(tag: Feature.Tag) {
    itemsByTag.remove(tag)?.let(mapItemAdapter::remove)
  }
}
