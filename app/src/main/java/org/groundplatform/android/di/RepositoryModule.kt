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
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationOfInterestRepositoryModule {
  @Binds
  abstract fun bindSurveyRepository(
    impl: LocationOfInterestRepository
  ): LocationOfInterestRepositoryInterface
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UserRepositoryModule {
  @Binds abstract fun bindSurveyRepository(impl: UserRepository): UserRepositoryInterface
}
