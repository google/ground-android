package com.google.android.gnd.mbtiles.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "grid_data")
public class GridData {
  @PrimaryKey
  @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
  public int zoom_level;

  @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
  public int tile_column;

  @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
  public int tile_row;

  @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
  public String key_name;

  @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
  public String key_json;

  public GridData() {
    // no-arg constructor
  }
}
