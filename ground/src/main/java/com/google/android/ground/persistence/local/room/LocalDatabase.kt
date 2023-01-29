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

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.google.android.ground.Config
import com.google.android.ground.persistence.local.room.converter.GeometryWrapperTypeConverter
import com.google.android.ground.persistence.local.room.converter.JsonArrayTypeConverter
import com.google.android.ground.persistence.local.room.converter.JsonObjectTypeConverter
import com.google.android.ground.persistence.local.room.converter.StyleTypeConverter
import com.google.android.ground.persistence.local.room.dao.*
import com.google.android.ground.persistence.local.room.entity.*
import com.google.android.ground.persistence.local.room.models.*

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
      LocationOfInterestEntity::class,
      LocationOfInterestMutationEntity::class,
      TaskEntity::class,
      JobEntity::class,
      MultipleChoiceEntity::class,
      OptionEntity::class,
      SurveyEntity::class,
      BaseMapEntity::class,
      SubmissionEntity::class,
      SubmissionMutationEntity::class,
      TileSetEntity::class,
      OfflineAreaEntity::class,
      UserEntity::class
    ],
  version = Config.DB_VERSION,
  exportSchema = false
)
@TypeConverters(
  TaskEntityType::class,
  MultipleChoiceEntityType::class,
  MutationEntityType::class,
  EntityState::class,
  GeometryWrapperTypeConverter::class,
  JsonArrayTypeConverter::class,
  JsonObjectTypeConverter::class,
  MutationEntitySyncStatus::class,
  OfflineAreaEntityState::class,
  StyleTypeConverter::class,
  TileSetEntityState::class
)
abstract class LocalDatabase : RoomDatabase() {
  abstract fun locationOfInterestDao(): LocationOfInterestDao
  abstract fun locationOfInterestMutationDao(): LocationOfInterestMutationDao
  abstract fun taskDao(): TaskDao
  abstract fun jobDao(): JobDao
  abstract fun multipleChoiceDao(): MultipleChoiceDao
  abstract fun optionDao(): OptionDao
  abstract fun surveyDao(): SurveyDao
  abstract fun baseMapDao(): BaseMapDao
  abstract fun submissionDao(): SubmissionDao
  abstract fun submissionMutationDao(): SubmissionMutationDao
  abstract fun tileSetDao(): TileSetDao
  abstract fun offlineAreaDao(): OfflineAreaDao
  abstract fun userDao(): UserDao
}
