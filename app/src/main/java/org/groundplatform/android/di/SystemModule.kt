/*
 * Copyright 2023 Google LLC
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

import android.content.Context
import android.location.Geocoder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.groundplatform.android.FirebaseCrashLogger
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.domain.system.CrashLogger
import org.groundplatform.domain.system.NetworkManagerInterface

@InstallIn(SingletonComponent::class)
@Module
object SystemModule {

  @Provides
  @Singleton
  fun provideGeocoder(@ApplicationContext context: Context): Geocoder {
    return Geocoder(context)
  }
}

@InstallIn(SingletonComponent::class)
@Module
abstract class NetworkManagerModule {
  @Binds
  @Singleton
  abstract fun bindNetworkManager(networkManager: NetworkManager): NetworkManagerInterface
}

@InstallIn(SingletonComponent::class)
@Module
abstract class CrashLoggerModule {
  @Binds @Singleton abstract fun bindCrashLogger(impl: FirebaseCrashLogger): CrashLogger
}
