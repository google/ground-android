package com.google.android.gnd.persistence.local.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import java.util.List;

@Dao
public interface ProjectDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insertOrUpdate(ProjectEntity project);

  @Query("SELECT * FROM project WHERE id = :id")
  Maybe<ProjectEntity> findById(String id);

  @Query("SELECT * FROM project")
  Maybe<List<ProjectEntity>> findAll();

  @Query("SELECT * FROM project WHERE is_active = 1 LIMIT 1")
  Maybe<ProjectEntity> getLastActiveProject();
}
