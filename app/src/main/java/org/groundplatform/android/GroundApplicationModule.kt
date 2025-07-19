/*
 * Copyright 2018 Google LLC
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
package org.groundplatform.android

import android.content.Context
import android.content.res.Resources
import com.google.android.gms.common.GoogleApiAvailability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Singleton
import org.groundplatform.android.data.repository.LocationOfInterestRepositoryImpl
import org.groundplatform.android.domain.repository.LocationOfInterestRepository
import org.groundplatform.android.persistence.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.groundplatform.android.persistence.remote.RemoteDataStore
import org.groundplatform.android.persistence.sync.MutationSyncWorkManager
import org.groundplatform.android.persistence.uuid.OfflineUuidGenerator
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.auth.AuthenticationManager
import org.groundplatform.android.ui.common.ViewModelModule

@InstallIn(SingletonComponent::class)
@Module(includes = [ViewModelModule::class])
object GroundApplicationModule {

  @Provides
  @Singleton
  fun googleApiAvailability(): GoogleApiAvailability {
    return GoogleApiAvailability.getInstance()
  }

  @Provides
  fun provideResources(@ApplicationContext context: Context): Resources {
    return context.resources
  }

  @Provides fun provideLocale() = Locale.getDefault()

  @Provides
  @Singleton
  fun provideLocationOfInterestRepository(
    authenticationManager: AuthenticationManager,
    localLoiStore: LocalLocationOfInterestStore,
    localSurveyStore: LocalSurveyStore,
    mutationSyncWorkManager: MutationSyncWorkManager,
    remoteDataStore: RemoteDataStore,
    userRepository: UserRepository,
    uuidGenerator: OfflineUuidGenerator,
  ): LocationOfInterestRepository {
    return LocationOfInterestRepositoryImpl(
      authenticationManager,
      localLoiStore,
      localSurveyStore,
      mutationSyncWorkManager,
      remoteDataStore,
      userRepository,
      uuidGenerator,
    )
  }
}
