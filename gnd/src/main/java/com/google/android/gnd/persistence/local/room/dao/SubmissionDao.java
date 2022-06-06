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
import com.google.android.gnd.persistence.local.room.entity.SubmissionEntity;
import com.google.android.gnd.persistence.local.room.models.EntityState;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;

@Dao
public interface SubmissionDao extends BaseDao<SubmissionEntity> {

  /**
   * Returns the submission with the specified UUID, if found.
   */
  @Query("SELECT * FROM submission WHERE id = :submissionId")
  Maybe<SubmissionEntity> findById(String submissionId);

  /**
   * Returns the list submissions associated with the specified feature, task and state.
   */
  @Query(
      "SELECT * FROM submission "
          + "WHERE feature_id = :featureId AND task_id = :taskId AND state = :state")
  Single<List<SubmissionEntity>> findByFeatureId(
      String featureId, String taskId, EntityState state);
}
