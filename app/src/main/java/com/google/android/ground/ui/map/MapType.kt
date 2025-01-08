/*
 * Copyright 2021 Google LLC
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

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.ground.R
import kotlinx.parcelize.Parcelize

/**
 * The type of base map shown beneath all other layers. Google Maps SDK refers to these as "map
 * type", or "map styles" in Mapbox, or "basemap" in Leaflet.
 */
@Parcelize
enum class MapType(
  @field:StringRes @StringRes val labelId: Int,
  @field:DrawableRes @DrawableRes val imageId: Int,
) : Parcelable {
  ROAD(R.string.road_map, R.drawable.map_type_roadmap),
  TERRAIN(R.string.terrain, R.drawable.map_type_terrain),
  SATELLITE(R.string.satellite, R.drawable.map_type_satellite);

  companion object {
    val DEFAULT = TERRAIN
  }
}
