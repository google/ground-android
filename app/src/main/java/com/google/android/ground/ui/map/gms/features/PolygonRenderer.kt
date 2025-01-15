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

import android.content.res.Resources
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.Polygon as MapsPolygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.ground.R
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.POLYGON_Z
import com.google.android.ground.ui.map.gms.toLatLng
import com.google.android.ground.ui.map.gms.toLatLngList
import javax.inject.Inject

class PolygonRenderer @Inject constructor(resources: Resources) :
  MapsItemRenderer<Polygon, MapsPolygon> {
  private val defaultStrokeWidth = resources.getDimension(R.dimen.line_geometry_width)

  override fun add(
    map: GoogleMap,
    tag: Feature.Tag,
    geometry: Polygon,
    style: Feature.Style,
    selected: Boolean,
    visible: Boolean,
  ): MapsPolygon {
    val strokeScale = if (selected) 2f else 1f
    val options = PolygonOptions()
    with(options) {
      addAll(geometry.shell.coordinates.map { it.toLatLng() })
      geometry.holes.forEach { addHole(it.coordinates.toLatLngList()) }
      clickable(false)
      visible(visible)
      zIndex(POLYGON_Z)
      strokeWidth(defaultStrokeWidth * strokeScale)
      strokeColor(style.color)
      strokeJointType(JointType.ROUND)
    }

    val mapsPolygon = map.addPolygon(options)
    mapsPolygon.tag = tag
    return mapsPolygon
  }
}
