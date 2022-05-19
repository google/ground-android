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
import com.google.android.gnd.persistence.local.room.entity.SubmissionMutationEntity;
import com.google.android.gnd.persistence.local.room.models.MutationEntitySyncStatus;
import com.google.android.gnd.rx.annotations.Cold;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.List;

/**
 * Data access object for database operations related to {@link SubmissionMutationEntity}.
 */
@Dao
public interface SubmissionMutationDao extends BaseDao<SubmissionMutationEntity> {

  @Query("SELECT * FROM submission_mutation")
  Flowable<List<SubmissionMutationEntity>> loadAllOnceAndStream();

  @Query(
      "SELECT * FROM submission_mutation "
          + "WHERE feature_id = :featureId AND state IN (:allowedStates)")
  Single<List<SubmissionMutationEntity>> findByFeatureId(
      String featureId, MutationEntitySyncStatus... allowedStates);

  @Query(
      "SELECT * FROM submission_mutation "
          + "WHERE submission_id = :submissionId AND state IN (:allowedStates)")
  Single<List<SubmissionMutationEntity>> findBySubmissionId(
      String submissionId, MutationEntitySyncStatus... allowedStates);

  @Cold(terminates = false)
  @Query(
      "SELECT * FROM submission_mutation "
          + "WHERE feature_id = :featureId AND state IN (:allowedStates)")
  Flowable<List<SubmissionMutationEntity>> findByFeatureIdOnceAndStream(
      String featureId, MutationEntitySyncStatus... allowedStates);
}
