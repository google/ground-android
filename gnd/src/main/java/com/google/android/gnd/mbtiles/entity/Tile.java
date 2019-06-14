package com.google.android.gnd.mapbox.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "tiles",
    indices = {
      @Index(
          value = {"zoom_level", "tile_column", "tile_row"},
          name = "tile_index",
          unique = true)
    })
public class Tile {
  @PrimaryKey
  @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
  public int zoom_level;

  @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
  public int tile_column;

  @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
  public int tile_row;

  @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
  public int tile_data;

  public Tile() {
    // no-arg constructor
  }
}
