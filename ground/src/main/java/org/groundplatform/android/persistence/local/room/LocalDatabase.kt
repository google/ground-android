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
package org.groundplatform.android.persistence.local.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.groundplatform.android.Config
import org.groundplatform.android.persistence.local.room.converter.GeometryWrapperTypeConverter
import org.groundplatform.android.persistence.local.room.converter.JsonArrayTypeConverter
import org.groundplatform.android.persistence.local.room.converter.JsonObjectTypeConverter
import org.groundplatform.android.persistence.local.room.converter.LoiPropertiesMapConverter
import org.groundplatform.android.persistence.local.room.converter.StyleTypeConverter
import org.groundplatform.android.persistence.local.room.dao.ConditionDao
import org.groundplatform.android.persistence.local.room.dao.DraftSubmissionDao
import org.groundplatform.android.persistence.local.room.dao.ExpressionDao
import org.groundplatform.android.persistence.local.room.dao.JobDao
import org.groundplatform.android.persistence.local.room.dao.LocationOfInterestDao
import org.groundplatform.android.persistence.local.room.dao.LocationOfInterestMutationDao
import org.groundplatform.android.persistence.local.room.dao.MultipleChoiceDao
import org.groundplatform.android.persistence.local.room.dao.OfflineAreaDao
import org.groundplatform.android.persistence.local.room.dao.OptionDao
import org.groundplatform.android.persistence.local.room.dao.SubmissionDao
import org.groundplatform.android.persistence.local.room.dao.SubmissionMutationDao
import org.groundplatform.android.persistence.local.room.dao.SurveyDao
import org.groundplatform.android.persistence.local.room.dao.TaskDao
import org.groundplatform.android.persistence.local.room.dao.UserDao
import org.groundplatform.android.persistence.local.room.entity.ConditionEntity
import org.groundplatform.android.persistence.local.room.entity.DraftSubmissionEntity
import org.groundplatform.android.persistence.local.room.entity.ExpressionEntity
import org.groundplatform.android.persistence.local.room.entity.JobEntity
import org.groundplatform.android.persistence.local.room.entity.LocationOfInterestEntity
import org.groundplatform.android.persistence.local.room.entity.LocationOfInterestMutationEntity
import org.groundplatform.android.persistence.local.room.entity.MultipleChoiceEntity
import org.groundplatform.android.persistence.local.room.entity.OfflineAreaEntity
import org.groundplatform.android.persistence.local.room.entity.OptionEntity
import org.groundplatform.android.persistence.local.room.entity.SubmissionEntity
import org.groundplatform.android.persistence.local.room.entity.SubmissionMutationEntity
import org.groundplatform.android.persistence.local.room.entity.SurveyEntity
import org.groundplatform.android.persistence.local.room.entity.TaskEntity
import org.groundplatform.android.persistence.local.room.entity.UserEntity
import org.groundplatform.android.persistence.local.room.fields.EntityDeletionState
import org.groundplatform.android.persistence.local.room.fields.ExpressionEntityType
import org.groundplatform.android.persistence.local.room.fields.MatchEntityType
import org.groundplatform.android.persistence.local.room.fields.MultipleChoiceEntityType
import org.groundplatform.android.persistence.local.room.fields.MutationEntitySyncStatus
import org.groundplatform.android.persistence.local.room.fields.MutationEntityType
import org.groundplatform.android.persistence.local.room.fields.OfflineAreaEntityState
import org.groundplatform.android.persistence.local.room.fields.TaskEntityType
import org.groundplatform.android.persistence.local.room.fields.TileSetEntityState

/**
 * Main entry point to local database API, exposing data access objects (DAOs) for interacting with
 * various entities persisted in tables in db.
 *
 * A separate data model is used to represent data stored locally to prevent leaking db-level design
 * details into main API * and to allow us to guarantee backwards compatibility.
 */
@Database(
  entities =
    [
      DraftSubmissionEntity::class,
      LocationOfInterestEntity::class,
      LocationOfInterestMutationEntity::class,
      TaskEntity::class,
      JobEntity::class,
      MultipleChoiceEntity::class,
      OptionEntity::class,
      SurveyEntity::class,
      SubmissionEntity::class,
      SubmissionMutationEntity::class,
      OfflineAreaEntity::class,
      UserEntity::class,
      ConditionEntity::class,
      ExpressionEntity::class,
    ],
  version = Config.DB_VERSION,
  exportSchema = true,
  autoMigrations = [AutoMigration(from = 120, to = 121)],
)
@TypeConverters(
  TaskEntityType::class,
  MultipleChoiceEntityType::class,
  MatchEntityType::class,
  ExpressionEntityType::class,
  MutationEntityType::class,
  EntityDeletionState::class,
  GeometryWrapperTypeConverter::class,
  JsonArrayTypeConverter::class,
  JsonObjectTypeConverter::class,
  MutationEntitySyncStatus::class,
  OfflineAreaEntityState::class,
  StyleTypeConverter::class,
  TileSetEntityState::class,
  LoiPropertiesMapConverter::class,
)
abstract class LocalDatabase : RoomDatabase() {
  abstract fun draftSubmissionDao(): DraftSubmissionDao

  abstract fun locationOfInterestDao(): LocationOfInterestDao

  abstract fun locationOfInterestMutationDao(): LocationOfInterestMutationDao

  abstract fun taskDao(): TaskDao

  abstract fun jobDao(): JobDao

  abstract fun multipleChoiceDao(): MultipleChoiceDao

  abstract fun optionDao(): OptionDao

  abstract fun surveyDao(): SurveyDao

  abstract fun submissionDao(): SubmissionDao

  abstract fun submissionMutationDao(): SubmissionMutationDao

  abstract fun offlineAreaDao(): OfflineAreaDao

  abstract fun userDao(): UserDao

  abstract fun conditionDao(): ConditionDao

  abstract fun expressionDao(): ExpressionDao
}
