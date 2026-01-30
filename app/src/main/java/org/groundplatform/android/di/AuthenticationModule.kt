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
package org.groundplatform.android.di

import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.groundplatform.android.BuildConfig
import org.groundplatform.android.system.auth.AnonymousAuthenticationManager
import org.groundplatform.android.system.auth.AuthenticationManager
import org.groundplatform.android.system.auth.GoogleAuthenticationManager

@InstallIn(SingletonComponent::class)
@Module
class AuthenticationModule {
  /** Provides appropriate implementation of authentication manager. */
  @Provides
  @Singleton
  fun authenticationManager(
    anonymousAuthenticationManager: AnonymousAuthenticationManager,
    googleAuthenticationManager: GoogleAuthenticationManager,
  ): AuthenticationManager =
    if (BuildConfig.USE_EMULATORS) anonymousAuthenticationManager else googleAuthenticationManager

  @Provides
  fun firebaseAuth(): FirebaseAuth {
    val auth = FirebaseAuth.getInstance()
    if (BuildConfig.USE_EMULATORS) {
      // Use the auth emulator so we can sign-in anonymously during dev.
      auth.useEmulator(BuildConfig.EMULATOR_HOST, BuildConfig.AUTH_EMULATOR_PORT)
    }
    return auth
  }
}
