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
package org.groundplatform.android.persistence.local

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.groundplatform.android.common.Constants
import org.groundplatform.android.util.allowThreadDiskReads

@InstallIn(SingletonComponent::class)
@Module
object SharedPreferencesModule {
  @Provides
  @Singleton
  fun sharedPreferences(@ApplicationContext context: Context): SharedPreferences =
    allowThreadDiskReads {
      context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Constants.SHARED_PREFS_MODE)
    }
}
