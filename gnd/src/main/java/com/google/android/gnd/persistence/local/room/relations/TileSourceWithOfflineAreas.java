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
