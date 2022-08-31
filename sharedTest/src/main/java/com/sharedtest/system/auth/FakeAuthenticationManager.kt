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

package com.sharedtest.system.auth;

import com.google.android.ground.model.User;
import com.google.android.ground.rx.annotations.Hot;
import com.google.android.ground.system.auth.AuthenticationManager;
import com.google.android.ground.system.auth.SignInState;
import com.sharedtest.FakeData;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FakeAuthenticationManager implements AuthenticationManager {

  @Hot(replays = true)
  private final Subject<SignInState> behaviourSubject = BehaviorSubject.create();

  // TODO(#1045): Allow default user to be initialized by tests.
  private User user = FakeData.USER;

  @Inject
  public FakeAuthenticationManager() {}

  @Override
  public Observable<SignInState> getSignInState() {
    return behaviourSubject;
  }

  @Override
  public User getCurrentUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public void setState(SignInState state) {
    behaviourSubject.onNext(state);
  }

  @Override
  public void init() {
    behaviourSubject.onNext(SignInState.signedIn(user));
  }

  @Override
  public void signIn() {
    behaviourSubject.onNext(SignInState.signedIn(user));
  }

  @Override
  public void signOut() {
    behaviourSubject.onNext(SignInState.signedOut());
  }
}
