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

package com.google.android.ground.repository;

import static com.google.android.ground.util.Enums.toEnum;

import com.google.android.ground.model.Role;
import com.google.android.ground.model.Survey;
import com.google.android.ground.model.User;
import com.google.android.ground.persistence.local.LocalDataStore;
import com.google.android.ground.persistence.local.LocalValueStore;
import com.google.android.ground.rx.Schedulers;
import com.google.android.ground.rx.annotations.Cold;
import com.google.android.ground.system.auth.AuthenticationManager;
import io.reactivex.Completable;
import io.reactivex.Single;
import javax.inject.Inject;

/**
 * Coordinates persistence of {@link User} instance in local data store. For more details on this
 * pattern and overall architecture, see * https://developer.android.com/jetpack/docs/guide.
 */
public class UserRepository {

  private final AuthenticationManager authenticationManager;
  private final LocalDataStore localDataStore;
  private final LocalValueStore localValueStore;
  private final Schedulers schedulers;

  @Inject
  public UserRepository(
      AuthenticationManager authenticationManager,
      LocalDataStore localDataStore,
      LocalValueStore localValueStore,
      Schedulers schedulers) {
    this.authenticationManager = authenticationManager;
    this.localDataStore = localDataStore;
    this.localValueStore = localValueStore;
    this.schedulers = schedulers;
  }

  public User getCurrentUser() {
    return authenticationManager.getCurrentUser();
  }

  public Role getUserRole(Survey survey) {
    String value = survey.getAcl().get(getCurrentUser().getEmail());
    return value == null ? Role.UNKNOWN : toEnum(Role.class, value);
  }

  @Cold
  public Completable saveUser(User user) {
    return localDataStore.insertOrUpdateUser(user).observeOn(schedulers.io());
  }

  @Cold
  public Single<User> getUser(String userId) {
    return localDataStore.getUser(userId);
  }

  /** Clears all user-specific preferences and settings. */
  public void clearUserPreferences() {
    localValueStore.clear();
  }
}
