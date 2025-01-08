/*
 * Copyright 2024 Google LLC
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
package com.google.android.ground.ui.map.gms.mog

import com.google.android.ground.Config
import com.google.android.ground.persistence.remote.RemoteStorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class MogProviderModule {

  private val defaultMogSources = Config.getMogSources(Config.DEFAULT_MOG_TILE_LOCATION)
  private val defaultMogCollection = MogCollection(defaultMogSources)

  @Provides
  fun provideMogClient(remoteStorageManager: RemoteStorageManager) =
    MogClient(defaultMogCollection, remoteStorageManager)
}
