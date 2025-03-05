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

package org.groundplatform.android.coroutines

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@InstallIn(SingletonComponent::class)
@Module
object CoroutinesScopesModule {
  @ApplicationScope
  @Singleton
  @Provides
  fun provideCoroutineScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob() + Dispatchers.Default)
  }

  @MainScope
  @Provides
  fun provideMainCoroutineScope(): CoroutineScope = kotlinx.coroutines.MainScope()
}

/**
 * Scope for jobs which need to outlive the lifecycle of specific view components. Use this scope to
 * bind jobs to the application's lifecycle instead.
 */
@Retention(AnnotationRetention.RUNTIME) @Qualifier annotation class ApplicationScope

@Retention(AnnotationRetention.RUNTIME) @Qualifier annotation class MainScope
