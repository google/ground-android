/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.data.repository

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.groundplatform.android.domain.repository.LocationOfInterestRepository
import org.groundplatform.android.persistence.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.groundplatform.android.persistence.remote.RemoteDataStore
import org.groundplatform.android.system.auth.AuthenticationManager

@InstallIn(SingletonComponent::class)
@Module
object RepositoryModule {

  @Provides
  @Singleton
  fun provideLocationOfInterestRepository(
    authenticationManager: AuthenticationManager,
    localLoiStore: LocalLocationOfInterestStore,
    localSurveyStore: LocalSurveyStore,
    remoteDataStore: RemoteDataStore,
  ): LocationOfInterestRepository {
    return LocationOfInterestRepositoryImpl(
      authenticationManager,
      localLoiStore,
      localSurveyStore,
      remoteDataStore,
    )
  }
}
