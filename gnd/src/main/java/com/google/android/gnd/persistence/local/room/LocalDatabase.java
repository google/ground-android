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
import com.google.android.gnd.persistence.local.room.converter.ResponseDeltasTypeConverter;
import com.google.android.gnd.persistence.local.room.converter.ResponseMapTypeConverter;
import com.google.android.gnd.persistence.local.room.converter.StyleTypeConverter;
import com.google.android.gnd.persistence.local.room.dao.FeatureDao;
import com.google.android.gnd.persistence.local.room.dao.FeatureMutationDao;
import com.google.android.gnd.persistence.local.room.dao.FieldDao;
import com.google.android.gnd.persistence.local.room.dao.FormDao;
import com.google.android.gnd.persistence.local.room.dao.LayerDao;
import com.google.android.gnd.persistence.local.room.dao.MultipleChoiceDao;
import com.google.android.gnd.persistence.local.room.dao.ObservationDao;
import com.google.android.gnd.persistence.local.room.dao.ObservationMutationDao;
import com.google.android.gnd.persistence.local.room.dao.OfflineBaseMapDao;
import com.google.android.gnd.persistence.local.room.dao.OfflineBaseMapSourceDao;
import com.google.android.gnd.persistence.local.room.dao.OptionDao;
import com.google.android.gnd.persistence.local.room.dao.ProjectDao;
import com.google.android.gnd.persistence.local.room.dao.TileSourceDao;
import com.google.android.gnd.persistence.local.room.dao.UserDao;
import com.google.android.gnd.persistence.local.room.entity.FeatureEntity;
import com.google.android.gnd.persistence.local.room.entity.FeatureMutationEntity;
import com.google.android.gnd.persistence.local.room.entity.FieldEntity;
import com.google.android.gnd.persistence.local.room.entity.FormEntity;
import com.google.android.gnd.persistence.local.room.entity.LayerEntity;
import com.google.android.gnd.persistence.local.room.entity.MultipleChoiceEntity;
import com.google.android.gnd.persistence.local.room.entity.ObservationEntity;
import com.google.android.gnd.persistence.local.room.entity.ObservationMutationEntity;
import com.google.android.gnd.persistence.local.room.entity.OfflineBaseMapEntity;
import com.google.android.gnd.persistence.local.room.entity.OfflineBaseMapSourceEntity;
import com.google.android.gnd.persistence.local.room.entity.OptionEntity;
import com.google.android.gnd.persistence.local.room.entity.ProjectEntity;
import com.google.android.gnd.persistence.local.room.entity.TileSourceEntity;
import com.google.android.gnd.persistence.local.room.entity.UserEntity;
import com.google.android.gnd.persistence.local.room.models.ElementEntityType;
import com.google.android.gnd.persistence.local.room.models.EntityState;
import com.google.android.gnd.persistence.local.room.models.FieldEntityType;
import com.google.android.gnd.persistence.local.room.models.MultipleChoiceEntityType;
import com.google.android.gnd.persistence.local.room.models.MutationEntityType;
import com.google.android.gnd.persistence.local.room.models.OfflineBaseMapEntityState;
import com.google.android.gnd.persistence.local.room.models.TileEntityState;

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
      FormEntity.class,
      LayerEntity.class,
      MultipleChoiceEntity.class,
      OptionEntity.class,
      ProjectEntity.class,
      OfflineBaseMapSourceEntity.class,
      ObservationEntity.class,
      ObservationMutationEntity.class,
      TileSourceEntity.class,
      OfflineBaseMapEntity.class,
      UserEntity.class
    },
    version = Config.DB_VERSION,
    exportSchema = false)
@TypeConverters({
  ElementEntityType.class,
  FieldEntityType.class,
  MultipleChoiceEntityType.class,
  MutationEntityType.class,
  EntityState.class,
  OfflineBaseMapEntityState.class,
  ResponseDeltasTypeConverter.class,
  ResponseMapTypeConverter.class,
  StyleTypeConverter.class,
  TileEntityState.class
})
public abstract class LocalDatabase extends RoomDatabase {

  public abstract FeatureDao featureDao();

  public abstract FeatureMutationDao featureMutationDao();

  public abstract FieldDao fieldDao();

  public abstract FormDao formDao();

  public abstract LayerDao layerDao();

  public abstract MultipleChoiceDao multipleChoiceDao();

  public abstract OptionDao optionDao();

  public abstract ProjectDao projectDao();

  public abstract OfflineBaseMapSourceDao offlineBaseMapSourceDao();

  public abstract ObservationDao observationDao();

  public abstract ObservationMutationDao observationMutationDao();

  public abstract TileSourceDao tileSourceDao();

  public abstract OfflineBaseMapDao offlineAreaDao();

  public abstract UserDao userDao();
}
