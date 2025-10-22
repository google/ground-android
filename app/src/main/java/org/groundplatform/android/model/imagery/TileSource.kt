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
package org.groundplatform.android.model.imagery

import org.groundplatform.android.model.map.Bounds

/**
 * Represents a single source of tiled map imagery used for displaying maps in the application.
 *
 * A [TileSource] defines where and how the map tiles are fetched — either from local offline
 * storage or from a remote source (e.g., MOG or online tile servers).
 */
sealed class TileSource

/**
 * Represents a locally stored set of map tiles, typically used for offline map rendering.
 *
 * These tiles are fetched from the device’s local storage and may be limited by bounding
 * coordinates and zoom levels.
 *
 * @property localFilePath The absolute file path on device storage where the local tile files are
 *   stored.
 * @property clipBounds The list of geographic bounding boxes (in lat/lng) within which these tiles
 *   are valid.
 * @property maxZoom The maximum zoom level supported by this offline tile source.
 */
data class LocalTileSource(
  val localFilePath: String,
  val clipBounds: List<Bounds>,
  val maxZoom: Int,
) : TileSource()

/**
 * Represents a remote map tile source hosted on a server, such as MOG (Map Over the Grid).
 *
 * This source type is typically used for streaming or caching map imagery over the network.
 *
 * @property remotePath The base URL or endpoint from which remote map tiles are fetched.
 */
data class RemoteMogTileSource(val remotePath: String) : TileSource()
