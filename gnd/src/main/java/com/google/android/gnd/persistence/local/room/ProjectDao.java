package com.google.android.gnd.persistence.local.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.Completable;
import io.reactivex.Maybe;

@Dao
public interface ProjectDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insertOrUpdate(FeatureEntity feature);

  @Query("SELECT * FROM project WHERE id = :id")
  Maybe<ProjectEntity> findById(String id);
}
