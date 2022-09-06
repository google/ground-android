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
package com.google.android.ground.persistence.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.persistence.local.room.models.TileSetEntityState

@Entity(tableName = "tile_sources")
data class TileSetEntity(
  @ColumnInfo(name = "id") @PrimaryKey val id: String,
  @ColumnInfo(name = "path") val path: String,
  @ColumnInfo(name = "url") val url: String,
  @ColumnInfo(name = "state") val state: TileSetEntityState,
  @ColumnInfo(name = "basemap_count") val offlineAreaReferenceCount: Int
) {

  companion object {
    fun toTileSet(tileSetEntity: TileSetEntity): TileSet {
      return TileSet(
        tileSetEntity.url,
        tileSetEntity.id,
        tileSetEntity.path,
        toTileState(tileSetEntity.state),
        tileSetEntity.offlineAreaReferenceCount
      )
    }

    private fun toTileState(state: TileSetEntityState): TileSet.State {
      return when (state) {
        TileSetEntityState.PENDING -> TileSet.State.PENDING
        TileSetEntityState.IN_PROGRESS -> TileSet.State.IN_PROGRESS
        TileSetEntityState.DOWNLOADED -> TileSet.State.DOWNLOADED
        TileSetEntityState.FAILED -> TileSet.State.FAILED
        else -> throw IllegalArgumentException("Unknown tile source state: $state")
      }
    }

    fun fromTileSet(tileSet: TileSet): TileSetEntity =
      TileSetEntity(
        id = tileSet.id,
        path = tileSet.path,
        state = toEntityState(tileSet.state),
        url = tileSet.url,
        offlineAreaReferenceCount = tileSet.offlineAreaReferenceCount
      )

    private fun toEntityState(state: TileSet.State): TileSetEntityState {
      return when (state) {
        TileSet.State.PENDING -> TileSetEntityState.PENDING
        TileSet.State.IN_PROGRESS -> TileSetEntityState.IN_PROGRESS
        TileSet.State.FAILED -> TileSetEntityState.FAILED
        TileSet.State.DOWNLOADED -> TileSetEntityState.DOWNLOADED
      }
    }
  }
}
