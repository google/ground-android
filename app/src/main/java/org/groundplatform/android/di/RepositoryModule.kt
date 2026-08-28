/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import org.groundplatform.android.di.coroutines.ApplicationScope
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.repository.SubmissionRepository
import org.groundplatform.android.repository.UserMediaRepository
import org.groundplatform.data.repository.MapStateRepository
import org.groundplatform.data.repository.MutationRepository
import org.groundplatform.data.repository.SurveyRepository
import org.groundplatform.data.repository.TermsOfServiceRepository
import org.groundplatform.data.repository.UserRepository
import org.groundplatform.data.stores.LocalDatabase
import org.groundplatform.data.stores.LocalLocationOfInterestStore
import org.groundplatform.data.stores.LocalSubmissionStore
import org.groundplatform.data.stores.LocalSurveyStore
import org.groundplatform.data.stores.LocalUserStore
import org.groundplatform.data.stores.LocalValueStore
import org.groundplatform.data.stores.RemoteDataStore
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.groundplatform.domain.repository.MapStateRepositoryInterface
import org.groundplatform.domain.repository.MutationRepositoryInterface
import org.groundplatform.domain.repository.OfflineAreaRepositoryInterface
import org.groundplatform.domain.repository.SubmissionRepositoryInterface
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.domain.repository.TermsOfServiceRepositoryInterface
import org.groundplatform.domain.repository.UserMediaRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.groundplatform.domain.system.CrashLogger
import org.groundplatform.domain.system.NetworkManagerInterface
import org.groundplatform.domain.system.auth.AuthenticationManager

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationOfInterestRepositoryModule {
  @Binds
  @Singleton
  abstract fun bindLocationOfInterestRepository(
    impl: LocationOfInterestRepository
  ): LocationOfInterestRepositoryInterface
}

@Module
@InstallIn(SingletonComponent::class)
object UserRepositoryModule {
  @Provides
  @Singleton
  fun provideUserRepository(
    authenticationManager: AuthenticationManager,
    localValueStore: LocalValueStore,
    localUserStore: LocalUserStore,
    networkManager: NetworkManagerInterface,
    surveyRepository: SurveyRepositoryInterface,
    remoteDataStore: RemoteDataStore,
    localDatabase: LocalDatabase,
  ): UserRepositoryInterface =
    UserRepository(
      authenticationManager,
      localValueStore,
      localUserStore,
      networkManager,
      surveyRepository,
      remoteDataStore,
      localDatabase,
    )
}

@Module
@InstallIn(SingletonComponent::class)
object SurveyRepositoryModule {
  @Provides
  @Singleton
  fun provideSurveyRepository(
    @ApplicationScope externalScope: CoroutineScope,
    crashLogger: CrashLogger,
    localSurveyStore: LocalSurveyStore,
    localValueStore: LocalValueStore,
    remoteDataStore: RemoteDataStore,
  ): SurveyRepositoryInterface =
    SurveyRepository(externalScope, crashLogger, localSurveyStore, localValueStore, remoteDataStore)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SubmissionRepositoryModule {
  @Binds
  @Singleton
  abstract fun bindSubmissionRepository(impl: SubmissionRepository): SubmissionRepositoryInterface
}

@Module
@InstallIn(SingletonComponent::class)
object MapStateRepositoryModule {
  @Provides
  @Singleton
  fun provideMapStateRepository(localValueStore: LocalValueStore): MapStateRepositoryInterface =
    MapStateRepository(localValueStore)
}

@Module
@InstallIn(SingletonComponent::class)
object MutationRepositoryModule {
  @Provides
  @Singleton
  fun provideMutationRepository(
    localLocationOfInterestStore: LocalLocationOfInterestStore,
    localSubmissionStore: LocalSubmissionStore,
    remoteDataStore: RemoteDataStore,
    userRepository: UserRepositoryInterface,
  ): MutationRepositoryInterface =
    MutationRepository(
      localLocationOfInterestStore,
      localSubmissionStore,
      remoteDataStore,
      userRepository,
    )
}

@Module
@InstallIn(SingletonComponent::class)
object TermsOfServiceRepositoryModule {
  @Provides
  @Singleton
  fun provideTermsOfServiceRepository(
    networkManager: NetworkManagerInterface,
    remoteDataStore: RemoteDataStore,
    localValueStore: LocalValueStore,
  ): TermsOfServiceRepositoryInterface =
    TermsOfServiceRepository(networkManager, remoteDataStore, localValueStore)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OfflineAreaRepositoryModule {
  @Binds
  @Singleton
  abstract fun bindOfflineAreaRepositoryRepository(
    impl: OfflineAreaRepository
  ): OfflineAreaRepositoryInterface
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UserMediaRepositoryModule {
  @Binds
  @Singleton
  abstract fun bindUserMediaRepository(impl: UserMediaRepository): UserMediaRepositoryInterface
}
