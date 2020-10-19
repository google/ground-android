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

package com.google.android.gnd.persistence.local.room.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Query;
import com.google.android.gnd.persistence.local.room.entity.OfflineBaseMapEntity;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import java.util.List;

/** Provides read/write operations for writing {@link OfflineBaseMapEntity} to the local db. */
@Dao
public interface OfflineBaseMapDao extends BaseDao<OfflineBaseMapEntity> {
  @NonNull
  @Query("SELECT * FROM offline_base_map")
  Flowable<List<OfflineBaseMapEntity>> findAllOnceAndStream();

  @NonNull
  @Query("SELECT * FROM offline_base_map WHERE id = :id")
  Maybe<OfflineBaseMapEntity> findById(String id);
}
