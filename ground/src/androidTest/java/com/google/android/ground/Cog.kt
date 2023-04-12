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

package com.google.android.ground

import mil.nga.tiff.TIFFImage

class Cog(private val tiff: TIFFImage) {
  val images = tiff.fileDirectories.map(::CogImage)
  val minZoomLevel = images.minBy { it.zoomLevel }
  val maxZoomLevel = images.maxBy { it.zoomLevel }
  val imagesByZoomLevel = images.map { it.zoomLevel to it }.toMap()
}
