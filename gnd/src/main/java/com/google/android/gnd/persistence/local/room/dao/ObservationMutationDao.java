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

import androidx.room.Dao;
import androidx.room.Query;
import com.google.android.gnd.persistence.local.room.entity.ObservationMutationEntity;
import com.google.android.gnd.persistence.local.room.models.MutationEntitySyncStatus;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.List;

/** Data access object for database operations related to {@link ObservationMutationEntity}. */
@Dao
public interface ObservationMutationDao extends BaseDao<ObservationMutationEntity> {
  @Query("SELECT * FROM observation_mutation")
  Flowable<List<ObservationMutationEntity>> loadAllOnceAndStream();

  @Query(
      "SELECT * FROM observation_mutation "
          + "WHERE feature_id = :featureId AND state IN (:allowedStates)")
  Single<List<ObservationMutationEntity>> findByFeatureId(
      String featureId, MutationEntitySyncStatus... allowedStates);

  @Query(
      "SELECT * FROM observation_mutation "
          + "WHERE observation_id = :observationId AND state IN (:allowedStates)")
  Single<List<ObservationMutationEntity>> findByObservationId(
      String observationId, MutationEntitySyncStatus... allowedStates);
}
