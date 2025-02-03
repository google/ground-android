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
package com.google.android.ground.persistence.local.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.google.android.ground.Config
import com.google.android.ground.persistence.local.room.converter.GeometryWrapperTypeConverter
import com.google.android.ground.persistence.local.room.converter.JsonArrayTypeConverter
import com.google.android.ground.persistence.local.room.converter.JsonObjectTypeConverter
import com.google.android.ground.persistence.local.room.converter.LoiPropertiesMapConverter
import com.google.android.ground.persistence.local.room.converter.StyleTypeConverter
import com.google.android.ground.persistence.local.room.dao.ConditionDao
import com.google.android.ground.persistence.local.room.dao.DraftSubmissionDao
import com.google.android.ground.persistence.local.room.dao.ExpressionDao
import com.google.android.ground.persistence.local.room.dao.JobDao
import com.google.android.ground.persistence.local.room.dao.LocationOfInterestDao
import com.google.android.ground.persistence.local.room.dao.LocationOfInterestMutationDao
import com.google.android.ground.persistence.local.room.dao.MultipleChoiceDao
import com.google.android.ground.persistence.local.room.dao.OfflineAreaDao
import com.google.android.ground.persistence.local.room.dao.OptionDao
import com.google.android.ground.persistence.local.room.dao.SubmissionDao
import com.google.android.ground.persistence.local.room.dao.SubmissionMutationDao
import com.google.android.ground.persistence.local.room.dao.SurveyDao
import com.google.android.ground.persistence.local.room.dao.TaskDao
import com.google.android.ground.persistence.local.room.dao.UserDao
import com.google.android.ground.persistence.local.room.entity.ConditionEntity
import com.google.android.ground.persistence.local.room.entity.DraftSubmissionEntity
import com.google.android.ground.persistence.local.room.entity.ExpressionEntity
import com.google.android.ground.persistence.local.room.entity.JobEntity
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestEntity
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestMutationEntity
import com.google.android.ground.persistence.local.room.entity.MultipleChoiceEntity
import com.google.android.ground.persistence.local.room.entity.OfflineAreaEntity
import com.google.android.ground.persistence.local.room.entity.OptionEntity
import com.google.android.ground.persistence.local.room.entity.SubmissionEntity
import com.google.android.ground.persistence.local.room.entity.SubmissionMutationEntity
import com.google.android.ground.persistence.local.room.entity.SurveyEntity
import com.google.android.ground.persistence.local.room.entity.TaskEntity
import com.google.android.ground.persistence.local.room.entity.UserEntity
import com.google.android.ground.persistence.local.room.fields.EntityDeletionState
import com.google.android.ground.persistence.local.room.fields.ExpressionEntityType
import com.google.android.ground.persistence.local.room.fields.MatchEntityType
import com.google.android.ground.persistence.local.room.fields.MultipleChoiceEntityType
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.room.fields.MutationEntityType
import com.google.android.ground.persistence.local.room.fields.OfflineAreaEntityState
import com.google.android.ground.persistence.local.room.fields.TaskEntityType
import com.google.android.ground.persistence.local.room.fields.TileSetEntityState

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
