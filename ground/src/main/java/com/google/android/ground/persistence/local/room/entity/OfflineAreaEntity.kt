/*
 * Copyright 2019 Google LLC
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.model.basemap.OfflineArea
import com.google.android.ground.persistence.local.room.models.OfflineAreaEntityState

/** Represents a [OfflineArea] in the local data store. */
@Entity(tableName = "offline_base_map")
data class OfflineAreaEntity(
  @ColumnInfo(name = "id") @PrimaryKey val id: String,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "state") val state: OfflineAreaEntityState,
  @ColumnInfo(name = "north") val north: Double,
  @ColumnInfo(name = "south") val south: Double,
  @ColumnInfo(name = "east") val east: Double,
  @ColumnInfo(name = "west") val west: Double
) {

  companion object {
    fun toArea(offlineAreaEntity: OfflineAreaEntity): OfflineArea {
      val northEast = LatLng(offlineAreaEntity.north, offlineAreaEntity.east)
      val southWest = LatLng(offlineAreaEntity.south, offlineAreaEntity.west)
      val bounds = LatLngBounds(southWest, northEast)
      return OfflineArea(
        offlineAreaEntity.id,
        toAreaState(offlineAreaEntity.state),
        bounds,
        offlineAreaEntity.name
      )
    }

    private fun toAreaState(state: OfflineAreaEntityState): OfflineArea.State =
      when (state) {
        OfflineAreaEntityState.PENDING -> OfflineArea.State.PENDING
        OfflineAreaEntityState.IN_PROGRESS -> OfflineArea.State.IN_PROGRESS
        OfflineAreaEntityState.DOWNLOADED -> OfflineArea.State.DOWNLOADED
        OfflineAreaEntityState.FAILED -> OfflineArea.State.FAILED
        else -> throw IllegalArgumentException("Unknown area state: $state")
      }

    fun fromArea(offlineArea: OfflineArea): OfflineAreaEntity =
      OfflineAreaEntity(
        id = offlineArea.id,
        state = toEntityState(offlineArea.state),
        name = offlineArea.name,
        north = offlineArea.bounds.northeast.latitude,
        east = offlineArea.bounds.northeast.longitude,
        south = offlineArea.bounds.southwest.latitude,
        west = offlineArea.bounds.southwest.longitude
      )

    private fun toEntityState(state: OfflineArea.State): OfflineAreaEntityState =
      when (state) {
        OfflineArea.State.PENDING -> OfflineAreaEntityState.PENDING
        OfflineArea.State.IN_PROGRESS -> OfflineAreaEntityState.IN_PROGRESS
        OfflineArea.State.FAILED -> OfflineAreaEntityState.FAILED
        OfflineArea.State.DOWNLOADED -> OfflineAreaEntityState.DOWNLOADED
      }
  }
}
