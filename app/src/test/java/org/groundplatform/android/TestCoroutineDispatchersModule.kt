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
package org.groundplatform.android

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.groundplatform.android.coroutines.CoroutineDispatchersModule
import org.groundplatform.android.coroutines.DefaultDispatcher
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.coroutines.MainDispatcher
import org.groundplatform.android.coroutines.MainImmediateDispatcher

@Module
@TestInstallIn(
  components = [SingletonComponent::class],
  replaces = [CoroutineDispatchersModule::class],
)
object TestCoroutineDispatchersModule {
  private val testDispatcher = StandardTestDispatcher()

  @Provides fun provideTestDispatcher(): TestDispatcher = testDispatcher

  @DefaultDispatcher @Provides fun provideDefaultDispatcher(): CoroutineDispatcher = testDispatcher

  @IoDispatcher @Provides fun provideIoDispatcher(): CoroutineDispatcher = testDispatcher

  @MainDispatcher @Provides fun provideMainDispatcher(): CoroutineDispatcher = testDispatcher

  @MainImmediateDispatcher
  @Provides
  fun provideMainImmediateDispatcher(): CoroutineDispatcher = testDispatcher
}
