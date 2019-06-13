package com.google.android.gnd.mapbox;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.google.android.gnd.mapbox.entity.Grid;
import com.google.android.gnd.mapbox.entity.GridData;
import com.google.android.gnd.mapbox.entity.Metadata;
import com.google.android.gnd.mapbox.entity.Tile;

@Database(
    version = 1,
    entities = {Metadata.class, Tile.class, Grid.class, GridData.class})
abstract class MapboxDatabase extends RoomDatabase {
  // TODO: Implement a Database for mbtiles.
  // Grid and GridData are optional tables.
}
