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
package org.groundplatform.android

import android.content.Context
import android.content.res.Resources
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.mockito.Mockito
import java.util.Locale

@Module
@TestInstallIn(
  components = [SingletonComponent::class],
  replaces = [GroundApplicationModule::class],
)
object TestFirebaseModule {
  @Provides
  fun provideGoogleApiAvailability(): GoogleApiAvailability = GoogleApiAvailability.getInstance()

  @Provides fun provideResources(@ApplicationContext ctx: Context): Resources = ctx.resources

  @Provides fun provideLocale(): Locale = Locale.getDefault()

  @Provides
  fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig =
    Mockito.mock(FirebaseRemoteConfig::class.java)
}
