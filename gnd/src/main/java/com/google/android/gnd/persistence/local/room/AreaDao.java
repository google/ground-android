/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gnd.persistence.local.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

/**
 * Provides read/write operations for writing {@link AreaEntity} to the local db.
 */
@Dao
public interface AreaDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  Completable insertOrUpdate(AreaEntity areaEntity);

  @Query("SELECT * FROM area")
  Flowable<List<AreaEntity>> findAll();

  @Query("SELECT * FROM area WHERE id = :id")
  Maybe<AreaEntity> findById(String id);

  @Query("SELECT * FROM area WHERE bounds = :bounds")
  Maybe<AreaEntity> findByBounds(LatLngBounds bounds);
}
