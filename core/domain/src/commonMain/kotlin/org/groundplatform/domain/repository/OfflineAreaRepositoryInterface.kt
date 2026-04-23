/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.domain.repository

import kotlinx.coroutines.flow.Flow
import org.groundplatform.domain.model.imagery.OfflineArea
import org.groundplatform.domain.model.imagery.TileSource
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.domain.model.util.ByteCount

/**
 * Corners of the viewport are scaled by this value when determining the name of downloaded areas.
 * Value derived experimentally.
 */
interface OfflineAreaRepositoryInterface {
  /**
   * Retrieves all offline areas from the local store and continually streams the list as the local
   * store is updated.
   */
  fun offlineAreas(): Flow<List<OfflineArea>>

  /** Fetches a single offline area by ID. */
  suspend fun getOfflineArea(offlineAreaId: String): OfflineArea?

  /**
   * Downloads tiles in the specified bounds and stores them in the local filesystem. Emits the
   * number of bytes processed and total expected bytes as the download progresses.
   */
  fun downloadTiles(bounds: Bounds): Flow<Pair<Int, Int>>

  fun getOfflineTileSourcesFlow(): Flow<TileSource>

  suspend fun hasHiResImagery(bounds: Bounds): Result<Boolean>

  suspend fun estimateSizeOnDisk(bounds: Bounds): Result<Int>

  /** Returns the number of bytes occupied by tiles on the local device. */
  fun sizeOnDevice(offlineArea: OfflineArea): ByteCount

  /**
   * Deletes the provided offline area from the device, including all associated unused tiles on the
   * local filesystem. Folders containing the deleted tiles are also removed if empty.
   */
  suspend fun removeFromDevice(offlineArea: OfflineArea)

  suspend fun removeAllOfflineAreas()
}
