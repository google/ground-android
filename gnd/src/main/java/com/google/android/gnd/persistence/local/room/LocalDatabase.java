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

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.google.android.gnd.Config;
import com.google.android.gnd.persistence.local.room.converter.JsonArrayTypeConverter;
import com.google.android.gnd.persistence.local.room.converter.JsonObjectTypeConverter;
import com.google.android.gnd.persistence.local.room.converter.StyleTypeConverter;
import com.google.android.gnd.persistence.local.room.dao.BaseMapDao;
import com.google.android.gnd.persistence.local.room.dao.FeatureDao;
import com.google.android.gnd.persistence.local.room.dao.FeatureMutationDao;
import com.google.android.gnd.persistence.local.room.dao.FieldDao;
import com.google.android.gnd.persistence.local.room.dao.TaskDao;
import com.google.android.gnd.persistence.local.room.dao.JobDao;
import com.google.android.gnd.persistence.local.room.dao.MultipleChoiceDao;
import com.google.android.gnd.persistence.local.room.dao.OfflineAreaDao;
import com.google.android.gnd.persistence.local.room.dao.OptionDao;
import com.google.android.gnd.persistence.local.room.dao.SurveyDao;
import com.google.android.gnd.persistence.local.room.dao.SubmissionDao;
import com.google.android.gnd.persistence.local.room.dao.SubmissionMutationDao;
import com.google.android.gnd.persistence.local.room.dao.TileSetDao;
import com.google.android.gnd.persistence.local.room.dao.UserDao;
import com.google.android.gnd.persistence.local.room.entity.BaseMapEntity;
import com.google.android.gnd.persistence.local.room.entity.FeatureEntity;
import com.google.android.gnd.persistence.local.room.entity.FeatureMutationEntity;
import com.google.android.gnd.persistence.local.room.entity.FieldEntity;
import com.google.android.gnd.persistence.local.room.entity.TaskEntity;
import com.google.android.gnd.persistence.local.room.entity.JobEntity;
import com.google.android.gnd.persistence.local.room.entity.MultipleChoiceEntity;
import com.google.android.gnd.persistence.local.room.entity.OfflineAreaEntity;
import com.google.android.gnd.persistence.local.room.entity.OptionEntity;
import com.google.android.gnd.persistence.local.room.entity.SurveyEntity;
import com.google.android.gnd.persistence.local.room.entity.SubmissionEntity;
import com.google.android.gnd.persistence.local.room.entity.SubmissionMutationEntity;
import com.google.android.gnd.persistence.local.room.entity.TileSetEntity;
import com.google.android.gnd.persistence.local.room.entity.UserEntity;
import com.google.android.gnd.persistence.local.room.models.StepEntityType;
import com.google.android.gnd.persistence.local.room.models.EntityState;
import com.google.android.gnd.persistence.local.room.models.FieldEntityType;
import com.google.android.gnd.persistence.local.room.models.MultipleChoiceEntityType;
import com.google.android.gnd.persistence.local.room.models.MutationEntitySyncStatus;
import com.google.android.gnd.persistence.local.room.models.MutationEntityType;
import com.google.android.gnd.persistence.local.room.models.OfflineAreaEntityState;
import com.google.android.gnd.persistence.local.room.models.TileSetEntityState;

/**
 * Main entry point to local database API, exposing data access objects (DAOs) for interacting with
 * various entities persisted in tables in db.
 *
 * <p>A separate data model is used to represent data stored locally to prevent leaking db-level
 * design details into main API * and to allow us to guarantee backwards compatibility.
 */
@Database(
    entities = {
        FeatureEntity.class,
        FeatureMutationEntity.class,
        FieldEntity.class,
        TaskEntity.class,
        JobEntity.class,
        MultipleChoiceEntity.class,
        OptionEntity.class,
        SurveyEntity.class,
        BaseMapEntity.class,
        SubmissionEntity.class,
        SubmissionMutationEntity.class,
        TileSetEntity.class,
        OfflineAreaEntity.class,
        UserEntity.class
    },
    version = Config.DB_VERSION,
    exportSchema = false)
@TypeConverters({
    StepEntityType.class,
    FieldEntityType.class,
    MultipleChoiceEntityType.class,
    MutationEntityType.class,
    EntityState.class,
    JsonArrayTypeConverter.class,
    JsonObjectTypeConverter.class,
    MutationEntitySyncStatus.class,
    OfflineAreaEntityState.class,
    StyleTypeConverter.class,
    TileSetEntityState.class
})
public abstract class LocalDatabase extends RoomDatabase {

  public abstract FeatureDao featureDao();

  public abstract FeatureMutationDao featureMutationDao();

  public abstract FieldDao fieldDao();

  public abstract TaskDao taskDao();

  public abstract JobDao jobDao();

  public abstract MultipleChoiceDao multipleChoiceDao();

  public abstract OptionDao optionDao();

  public abstract SurveyDao surveyDao();

  public abstract BaseMapDao baseMapDao();

  public abstract SubmissionDao submissionDao();

  public abstract SubmissionMutationDao submissionMutationDao();

  public abstract TileSetDao tileSetDao();

  public abstract OfflineAreaDao offlineAreaDao();

  public abstract UserDao userDao();
}
