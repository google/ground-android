/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground.model.imagery

import com.google.android.ground.ui.map.Bounds

/** Represents a single source of tiled map imagery. */
data class TileSource(val url: String, val type: Type, val clipBounds: List<Bounds>?) {
  // TODO(#1790): Consider creating specialized classes for each type of TileSource.
  enum class Type {
    TILED_WEB_MAP,
    MOG_COLLECTION,
    UNKNOWN,
  }
}
