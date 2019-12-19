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

/**
 * Main entry point to local database API, exposing data access objects (DAOs) for interacting with
 * various entities persisted in tables in db.
 *
 * <p>A separate data model is used to represent data stored locally to prevent leaking db-level
 * design details into main API * and to allow us to guarantee backwards compatibility.
 */
// TODO: Make injectable via Dagger.
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
      RecordEntity.class,
      RecordMutationEntity.class,
      TileEntity.class,
      OfflineAreaEntity.class,
    },
    // TODO(#128): Reset version to 1 before releasing.
    version = 32,
    exportSchema = false)
// CHECKSTYLE IGNORE indentation FOR NEXT 12 LINES
@TypeConverters({
  ElementEntityType.class,
  FieldEntityType.class,
  MultipleChoiceEntityType.class,
  MutationEntityType.class,
  EntityState.class,
  OfflineAreaEntityState.class,
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

  public abstract RecordDao recordDao();

  public abstract RecordMutationDao recordMutationDao();

  public abstract TileDao tileDao();

  public abstract OfflineAreaDao offlineAreaDao();
}
