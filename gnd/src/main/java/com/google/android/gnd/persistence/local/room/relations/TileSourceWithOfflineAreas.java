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

package com.google.android.gnd.persistence.local.room.relations;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;
import com.google.android.gnd.persistence.local.room.entity.OfflineAreaEntity;
import com.google.android.gnd.persistence.local.room.entity.OfflineAreaTileSourceCrossRef;
import com.google.android.gnd.persistence.local.room.entity.TileSourceEntity;
import java.util.List;

/** Represents relationships between tile sources and offline areas. */
public class TileSourceWithOfflineAreas {
  @Embedded public TileSourceEntity tileSourceEntity;

  @Relation(
      parentColumn = "path",
      entityColumn = "id",
      associateBy = @Junction(OfflineAreaTileSourceCrossRef.class))
  public List<OfflineAreaEntity> offlineAreaEntities;
}
