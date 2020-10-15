/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.persistence.local;

import com.google.android.gnd.persistence.local.room.LocalDatabase;
import com.google.android.gnd.persistence.local.room.RoomLocalDataStore;
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
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ApplicationComponent;
import javax.inject.Singleton;

@InstallIn(ApplicationComponent.class)
@Module
public abstract class LocalDataStoreModule {

  @Provides
  static FeatureDao featureDao(LocalDatabase localDatabase) {
    return localDatabase.featureDao();
  }

  @Provides
  static FeatureMutationDao featureMutationDao(LocalDatabase localDatabase) {
    return localDatabase.featureMutationDao();
  }

  @Provides
  static FieldDao fieldDao(LocalDatabase localDatabase) {
    return localDatabase.fieldDao();
  }

  @Provides
  static FormDao formDao(LocalDatabase localDatabase) {
    return localDatabase.formDao();
  }

  @Provides
  static LayerDao layerDao(LocalDatabase localDatabase) {
    return localDatabase.layerDao();
  }

  @Provides
  static MultipleChoiceDao multipleChoiceDao(LocalDatabase localDatabase) {
    return localDatabase.multipleChoiceDao();
  }

  @Provides
  static OptionDao optionDao(LocalDatabase localDatabase) {
    return localDatabase.optionDao();
  }

  @Provides
  static ProjectDao projectDao(LocalDatabase localDatabase) {
    return localDatabase.projectDao();
  }

  @Provides
  static ObservationDao observationDao(LocalDatabase localDatabase) {
    return localDatabase.observationDao();
  }

  @Provides
  static ObservationMutationDao observationMutationDao(LocalDatabase localDatabase) {
    return localDatabase.observationMutationDao();
  }

  @Provides
  static TileSourceDao tileSourceDao(LocalDatabase localDatabase) {
    return localDatabase.tileSourceDao();
  }

  @Provides
  static OfflineBaseMapDao offlineAreaDao(LocalDatabase localDatabase) {
    return localDatabase.offlineAreaDao();
  }

  @Provides
  static OfflineBaseMapSourceDao offlineBaseMapSourceDao(LocalDatabase localDatabase) {
    return localDatabase.offlineBaseMapSourceDao();
  }

  @Provides
  static UserDao userDao(LocalDatabase localDatabase) {
    return localDatabase.userDao();
  }

  /** Provides the Room implementation of local data store. */
  @Binds
  @Singleton
  abstract LocalDataStore localDataStore(RoomLocalDataStore ds);
}
