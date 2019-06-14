package com.google.android.gnd.mapbox.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.google.android.gnd.mapbox.entity.Tile;

import io.reactivex.Flowable;
import io.reactivex.Maybe;

@Dao
public interface TileDao {
  @Query(
      "SELECT * FROM tiles WHERE zoom_level=:zoom_level AND tile_column=:tile_column AND tile_row=:tile_row LIMIT 1")
  public Maybe<Tile> getTile(int zoom_level, int tile_column, int tile_row);

  @Query("SELECT * FROM tiles WHERE zoom_level=:zoom_level")
  public Flowable<Tile> getTilesAtZoomLevel(int zoom_level);
}
