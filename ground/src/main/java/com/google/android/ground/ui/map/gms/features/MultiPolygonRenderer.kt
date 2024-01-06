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
package com.google.android.ground.ui.map.gms.features

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polygon as MapsPolygon
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.ui.map.Feature
import javax.inject.Inject

class MultiPolygonRenderer @Inject constructor(private val polygonRenderer: PolygonRenderer) :
  MapItemRenderer<MultiPolygon, List<MapsPolygon>> {
  override fun addMapItem(
    map: GoogleMap,
    tag: Feature.Tag,
    geometry: MultiPolygon,
    style: Feature.Style
  ): List<MapsPolygon> = geometry.polygons.map { polygonRenderer.addMapItem(map, tag, it, style) }

  override fun remove(mapItem: List<Polygon>) {
    mapItem.forEach(polygonRenderer::remove)
  }
}
