/*
 * Copyright 2019 Google LLC
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

package com.google.android.gnd.repository.local;

import com.google.android.gnd.vo.Feature;
import io.reactivex.Single;

/**
 * Provides access to local persistent data store. This is the app's canonical store for latest
 * state; local changes are saved here before being sync'ed with remote stores, and remote updates
 * are persistent here before being made visible to the user.
 */
public interface LocalDataStore {

  /**
   * Adds a new {@link Feature} to the local db, queueing it to be created in the remote data store.
   *
   * @param feature the new feature to be created. Its localId and remoteId fields are ignored.
   * @return once local writes are complete, emits the new feature with a generated localId set.
   */
  Single<Feature> createNewFeature(Feature feature);
}
