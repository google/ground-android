package com.google.android.gnd.persistence.local.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

@Dao
public interface AreaDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  Completable insertOrUpdate(AreaEntity areaEntity);

  @Query("SELECT * FROM area")
  Flowable<List<AreaEntity>> findAll();

  @Query("SELECT * FROM area WHERE id = :id")
  Maybe<AreaEntity> findById(String id);
}
