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
import com.google.android.gnd.persistence.local.room.dao.BaseMapDao;
import com.google.android.gnd.persistence.local.room.dao.FeatureDao;
import com.google.android.gnd.persistence.local.room.dao.FeatureMutationDao;
import com.google.android.gnd.persistence.local.room.dao.FieldDao;
import com.google.android.gnd.persistence.local.room.dao.JobDao;
import com.google.android.gnd.persistence.local.room.dao.MultipleChoiceDao;
import com.google.android.gnd.persistence.local.room.dao.OfflineAreaDao;
import com.google.android.gnd.persistence.local.room.dao.OptionDao;
import com.google.android.gnd.persistence.local.room.dao.SubmissionDao;
import com.google.android.gnd.persistence.local.room.dao.SubmissionMutationDao;
import com.google.android.gnd.persistence.local.room.dao.SurveyDao;
import com.google.android.gnd.persistence.local.room.dao.TaskDao;
import com.google.android.gnd.persistence.local.room.dao.TileSetDao;
import com.google.android.gnd.persistence.local.room.dao.UserDao;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@InstallIn(SingletonComponent.class)
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
  static TaskDao taskDao(LocalDatabase localDatabase) {
    return localDatabase.taskDao();
  }

  @Provides
  static JobDao jobDao(LocalDatabase localDatabase) {
    return localDatabase.jobDao();
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
  static SurveyDao surveyDao(LocalDatabase localDatabase) {
    return localDatabase.surveyDao();
  }

  @Provides
  static SubmissionDao submissionDao(LocalDatabase localDatabase) {
    return localDatabase.submissionDao();
  }

  @Provides
  static SubmissionMutationDao submissionMutationDao(LocalDatabase localDatabase) {
    return localDatabase.submissionMutationDao();
  }

  @Provides
  static TileSetDao tileSetDao(LocalDatabase localDatabase) {
    return localDatabase.tileSetDao();
  }

  @Provides
  static OfflineAreaDao offlineAreaDao(LocalDatabase localDatabase) {
    return localDatabase.offlineAreaDao();
  }

  @Provides
  static BaseMapDao baseMapDao(LocalDatabase localDatabase) {
    return localDatabase.baseMapDao();
  }

  @Provides
  static UserDao userDao(LocalDatabase localDatabase) {
    return localDatabase.userDao();
  }

  /**
   * Provides the Room implementation of local data store.
   */
  @Binds
  @Singleton
  abstract LocalDataStore localDataStore(RoomLocalDataStore ds);
}
