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

package com.google.android.ground.cog

import com.google.android.gms.maps.model.LatLng
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate

object CoordinateTransformer {
  private val crsFactory = CRSFactory()
  private val webMercator = crsFactory.createFromName("epsg:3857")
  private val wgs84 = crsFactory.createFromName("epsg:4326")
  private val ctFactory = CoordinateTransformFactory()
  private val webMercatorToWgs84 = ctFactory.createTransform(webMercator, wgs84)

  fun webMercatorToWgs84(x: Double, y: Double): LatLng {
    val result = webMercatorToWgs84.transform(ProjCoordinate(x, y), ProjCoordinate())
    return LatLng(result.y, result.x)
  }
}
