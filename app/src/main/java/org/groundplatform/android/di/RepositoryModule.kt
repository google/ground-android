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
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.repository.MutationRepository
import org.groundplatform.android.repository.SubmissionRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.groundplatform.domain.repository.MapStateRepositoryInterface
import org.groundplatform.domain.repository.MutationRepositoryInterface
import org.groundplatform.domain.repository.SubmissionRepositoryInterface
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface

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
abstract class UserRepositoryModule {
  @Binds @Singleton abstract fun bindUserRepository(impl: UserRepository): UserRepositoryInterface
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SurveyRepositoryModule {
  @Binds
  @Singleton
  abstract fun bindSurveyRepository(impl: SurveyRepository): SurveyRepositoryInterface
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
abstract class MapStateRepositoryModule {
  @Binds
  @Singleton
  abstract fun bindMapStateRepository(impl: MapStateRepository): MapStateRepositoryInterface
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MutationRepositoryModule {
  @Binds
  @Singleton
  abstract fun bindMutationRepository(impl: MutationRepository): MutationRepositoryInterface
}
