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

package org.groundplatform.android.ui.map.gms.mog

enum class TiffTag(val id: Int, val isArray: Boolean) {
  ImageLength(257, false),
  ImageWidth(256, false),
  PhotometricInterpretation(262, false),
  JPEGTables(347, true),
  TileByteCounts(325, true),
  TileLength(323, false),
  TileOffsets(324, true),
  TileWidth(322, false),
  GdalNodata(42113, false);

  /**
   * Get the tag id.
   *
   * @return tag id
   */
  companion object {
    val byId: Map<Int, TiffTag> = entries.associateBy { it.id }
  }
}
