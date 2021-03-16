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

package com.google.android.gnd.repository;

import com.google.android.gnd.model.User;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.rx.annotations.Cold;
import io.reactivex.Completable;
import javax.inject.Inject;

/**
 * Coordinates persistence of {@link User} instance in local data store. For more details on this
 * pattern and overall architecture, see * https://developer.android.com/jetpack/docs/guide.
 */
public class UserRepository {

  private final LocalDataStore localDataStore;
  private final Schedulers schedulers;

  @Inject
  UserRepository(LocalDataStore localDataStore, Schedulers schedulers) {
    this.localDataStore = localDataStore;
    this.schedulers = schedulers;
  }

  @Cold
  public Completable saveUser(User user) {
    return localDataStore.insertOrUpdateUser(user).observeOn(schedulers.io());
  }
}
