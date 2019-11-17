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
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;

@Dao
public interface RecordDao {

  /**
   * Saves the provided entity into the record table, creating a new row or updating existing row as
   * necessary. Record id must already be assigned to a valid UUID, or the returned Completable will
   * terminate in error.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insertOrUpdate(RecordEntity record);

  /** Returns the record with the specified UUID, if found. */
  @Query("SELECT * FROM record WHERE id = :recordId")
  Maybe<RecordEntity> findById(String recordId);

  /**
   * Returns the list records associated with the specified feature and form, ignoring deleted
   * records (i.e., returns only records with state = State.DEFAULT (1)).
   */
  @Query("SELECT * FROM record WHERE feature_id = :featureId AND form_id = :formId AND state = 1")
  Single<List<RecordEntity>> findByFeatureId(String featureId, String formId);
}
