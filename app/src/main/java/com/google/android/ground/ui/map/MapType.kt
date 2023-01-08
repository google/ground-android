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
import kotlinx.parcelize.Parcelize

/**
 * MapType refers to the basemap shown below map LOIs and offline satellite imagery. It's called
 * "map styles" in Mapbox and "basemaps" in Leaflet.
 */
@Parcelize
data class MapType(
  val type: Int,
  @field:StringRes @param:StringRes val labelId: Int,
  @field:DrawableRes @param:DrawableRes val imageId: Int
) : Parcelable
