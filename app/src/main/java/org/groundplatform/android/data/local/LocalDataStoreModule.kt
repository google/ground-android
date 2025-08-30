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
package org.groundplatform.android.data.local

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.groundplatform.android.data.local.room.LocalDatabase
import org.groundplatform.android.data.local.room.dao.ConditionDao
import org.groundplatform.android.data.local.room.dao.DraftSubmissionDao
import org.groundplatform.android.data.local.room.dao.ExpressionDao
import org.groundplatform.android.data.local.room.dao.JobDao
import org.groundplatform.android.data.local.room.dao.LocationOfInterestDao
import org.groundplatform.android.data.local.room.dao.LocationOfInterestMutationDao
import org.groundplatform.android.data.local.room.dao.MultipleChoiceDao
import org.groundplatform.android.data.local.room.dao.OfflineAreaDao
import org.groundplatform.android.data.local.room.dao.OptionDao
import org.groundplatform.android.data.local.room.dao.SubmissionDao
import org.groundplatform.android.data.local.room.dao.SubmissionMutationDao
import org.groundplatform.android.data.local.room.dao.SurveyDao
import org.groundplatform.android.data.local.room.dao.TaskDao
import org.groundplatform.android.data.local.room.dao.UserDao
import org.groundplatform.android.data.local.room.stores.RoomLocationOfInterestStore
import org.groundplatform.android.data.local.room.stores.RoomOfflineAreaStore
import org.groundplatform.android.data.local.room.stores.RoomSubmissionStore
import org.groundplatform.android.data.local.room.stores.RoomSurveyStore
import org.groundplatform.android.data.local.room.stores.RoomUserStore
import org.groundplatform.android.data.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.data.local.stores.LocalOfflineAreaStore
import org.groundplatform.android.data.local.stores.LocalSubmissionStore
import org.groundplatform.android.data.local.stores.LocalSurveyStore
import org.groundplatform.android.data.local.stores.LocalUserStore

@InstallIn(SingletonComponent::class)
@Module
abstract class LocalDataStoreModule {

  @Binds
  @Singleton
  abstract fun localLocationOfInterestStore(
    store: RoomLocationOfInterestStore
  ): LocalLocationOfInterestStore

  @Binds
  @Singleton
  abstract fun offlineAreaStore(store: RoomOfflineAreaStore): LocalOfflineAreaStore

  @Binds @Singleton abstract fun submissionStore(store: RoomSubmissionStore): LocalSubmissionStore

  @Binds @Singleton abstract fun surveyStore(store: RoomSurveyStore): LocalSurveyStore

  @Binds @Singleton abstract fun userStore(store: RoomUserStore): LocalUserStore

  companion object {
    @Provides
    fun draftSubmissionDao(localDatabase: LocalDatabase): DraftSubmissionDao {
      return localDatabase.draftSubmissionDao()
    }

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
    fun offlineAreaDao(localDatabase: LocalDatabase): OfflineAreaDao {
      return localDatabase.offlineAreaDao()
    }

    @Provides
    fun userDao(localDatabase: LocalDatabase): UserDao {
      return localDatabase.userDao()
    }

    @Provides
    fun conditionDao(localDatabase: LocalDatabase): ConditionDao {
      return localDatabase.conditionDao()
    }

    @Provides
    fun expressionDao(localDatabase: LocalDatabase): ExpressionDao {
      return localDatabase.expressionDao()
    }
  }
}
