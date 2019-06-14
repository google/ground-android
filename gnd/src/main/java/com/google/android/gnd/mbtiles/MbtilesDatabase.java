package com.google.android.gnd.mbtiles;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.google.android.gnd.mbtiles.entity.Grid;
import com.google.android.gnd.mbtiles.entity.GridData;
import com.google.android.gnd.mbtiles.entity.Metadata;
import com.google.android.gnd.mbtiles.entity.Tile;

@Database(
    version = 1,
    entities = {Metadata.class, Tile.class, Grid.class, GridData.class})
abstract class MbtilesDatabase extends RoomDatabase {
  // TODO: Implement a Database for mbtiles.
  // Grid and GridData are optional tables.
}
