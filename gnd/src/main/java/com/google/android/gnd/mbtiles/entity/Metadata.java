package com.google.android.gnd.mapbox.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.util.TableInfo;

@Entity(tableName = "metadata")
public class Metadata {
  @PrimaryKey
  @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
  public String name;

  @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
  public String value;

  public Metadata() {
    // no-arg constructor
  }
}
