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
package com.google.android.ground.persistence.local

import com.google.android.ground.persistence.local.room.LocalDatabase
import com.google.android.ground.persistence.local.room.RoomLocalDataStore
import com.google.android.ground.persistence.local.room.dao.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class LocalDataStoreModule {

  @Binds @Singleton abstract fun localDataStore(ds: RoomLocalDataStore): LocalDataStore

  companion object {
    @Provides
    fun locationOfInterestDao(localDatabase: LocalDatabase): LocationOfInterestDao {
      return localDatabase.locationOfInterestDao()
    }

    @Provides
    fun locationOfInterestMutationDao(localDatabase: LocalDatabase): LocationOfInterestMutationDao {
      return localDatabase.locationOfInterestMutationDao()
    }

    @Provides
    fun taskDao(localDatabase: LocalDatabase): TaskDao {
      return localDatabase.taskDao()
    }

    @Provides
    fun jobDao(localDatabase: LocalDatabase): JobDao {
      return localDatabase.jobDao()
    }

    @Provides
    fun multipleChoiceDao(localDatabase: LocalDatabase): MultipleChoiceDao {
      return localDatabase.multipleChoiceDao()
    }

    @Provides
    fun optionDao(localDatabase: LocalDatabase): OptionDao {
      return localDatabase.optionDao()
    }

    @Provides
    fun surveyDao(localDatabase: LocalDatabase): SurveyDao {
      return localDatabase.surveyDao()
    }

    @Provides
    fun submissionDao(localDatabase: LocalDatabase): SubmissionDao {
      return localDatabase.submissionDao()
    }

    @Provides
    fun submissionMutationDao(localDatabase: LocalDatabase): SubmissionMutationDao {
      return localDatabase.submissionMutationDao()
    }

    @Provides
    fun tileSetDao(localDatabase: LocalDatabase): TileSetDao {
      return localDatabase.tileSetDao()
    }

    @Provides
    fun offlineAreaDao(localDatabase: LocalDatabase): OfflineAreaDao {
      return localDatabase.offlineAreaDao()
    }

    @Provides
    fun baseMapDao(localDatabase: LocalDatabase): BaseMapDao {
      return localDatabase.baseMapDao()
    }

    @Provides
    fun userDao(localDatabase: LocalDatabase): UserDao {
      return localDatabase.userDao()
    }
  }
}
