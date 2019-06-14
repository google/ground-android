package com.google.android.gnd.mbtiles.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "grids")
public class Grid {
  @PrimaryKey
  @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
  public int zoom_level;

  @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
  public int tile_column;

  @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
  public int tile_row;

  @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
  public int grid;

  public Grid() {
    // no-arg constructor
  }
}
