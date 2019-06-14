package com.google.android.gnd.mbtiles.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.google.android.gnd.mbtiles.entity.Metadata;

import io.reactivex.Maybe;

@Dao
public interface MetadataDao {
  // TODO: Figure out whether or not we even need insert methods.
  // Presumably we might, in order to load up the data specified in an mbtiles file, but we may
  // be able to do this automatically using a room db constructor.
  @Insert
  public void insertMetadata(Metadata metadata);

  @Query("SELECT * FROM metadata WHERE name=:metadataName")
  public Maybe<Metadata> getMetadata(String metadataName);
}
