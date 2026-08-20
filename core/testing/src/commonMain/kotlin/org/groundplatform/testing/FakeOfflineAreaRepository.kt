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
package org.groundplatform.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.groundplatform.domain.model.imagery.OfflineArea
import org.groundplatform.domain.model.imagery.TileSource
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.domain.model.util.ByteCount
import org.groundplatform.domain.repository.OfflineAreaRepositoryInterface

class FakeOfflineAreaRepository : OfflineAreaRepositoryInterface {
  var downloadedAreas: List<OfflineArea> = emptyList()

  var hasHiResImagery = false
  var estimatedSizeOnDisk = 0
  var sizeOnDevice: ByteCount = 0

  override fun offlineAreas(): Flow<List<OfflineArea>> = flowOf(downloadedAreas)

  override suspend fun getOfflineArea(offlineAreaId: String): OfflineArea? = downloadedAreas.find {
    it.id == offlineAreaId
  }

  override fun downloadTiles(bounds: Bounds): Flow<Pair<Int, Int>> = emptyFlow()

  override fun getOfflineTileSourcesFlow(): Flow<TileSource> = emptyFlow()

  override suspend fun hasHiResImagery(bounds: Bounds): Result<Boolean> =
    Result.success(hasHiResImagery)

  override suspend fun estimateSizeOnDisk(bounds: Bounds): Result<Int> =
    Result.success(estimatedSizeOnDisk)

  override fun sizeOnDevice(offlineArea: OfflineArea): ByteCount = sizeOnDevice

  override suspend fun removeFromDevice(offlineArea: OfflineArea) {
    downloadedAreas = downloadedAreas.filterNot { it.id == offlineArea.id }
  }

  override suspend fun removeAllOfflineAreas() {
    downloadedAreas = emptyList()
  }
}
