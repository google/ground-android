package com.google.android.gnd.persistence.local.room.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import com.google.auto.value.AutoValue;

/** Entity object that represents an association between offline areas and tile sources. */
@AutoValue
@Entity(
    primaryKeys = {"id", "path"},
    tableName = "offline_area_tile_sources_cross_ref")
public abstract class OfflineAreaTileSourceCrossRef {
  @AutoValue.CopyAnnotations
  @NonNull
  @ColumnInfo(name = "id")
  public abstract String getOfflineAreaId();

  @AutoValue.CopyAnnotations
  @NonNull
  @ColumnInfo(name = "path")
  public abstract String getTileSourcePath();

  public static Builder builder() {
    return new AutoValue_OfflineAreaTileSourceCrossRef.Builder();
  }

  public static OfflineAreaTileSourceCrossRef create(String offlineAreaId, String tileSourcePath) {
    return builder().setOfflineAreaId(offlineAreaId).setTileSourcePath(tileSourcePath).build();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setOfflineAreaId(String id);

    public abstract Builder setTileSourcePath(String path);

    public abstract OfflineAreaTileSourceCrossRef build();
  }
}
