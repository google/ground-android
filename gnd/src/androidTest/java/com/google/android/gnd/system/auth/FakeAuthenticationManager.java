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

package com.google.android.gnd.system.auth;

import com.google.android.gnd.model.User;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.auth.SignInState.State;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;

public class FakeAuthenticationManager implements AuthenticationManager {

  private static final String TAG = FakeAuthenticationManager.class.toString();

  @Hot(replays = true)
  public Subject<SignInState> behaviourSubject = BehaviorSubject.create();

  public static final User TEST_USER = User.builder()
      .setDisplayName("Test User")
      .setEmail("test@user.com")
      .setId("TEST_USER_ID")
      .build();

  @Inject
  public FakeAuthenticationManager() {
  }

  @Override
  public Observable<SignInState> getSignInState() {
    return behaviourSubject;
  }

  @Override
  public User getCurrentUser() {
    return TEST_USER;
  }

  @Override
  public void init() {
    behaviourSubject.onNext(new SignInState(TEST_USER));
  }

  @Override
  public void signIn() {
    behaviourSubject.onNext(new SignInState(TEST_USER));
  }

  @Override
  public void signOut() {
    behaviourSubject.onNext(new SignInState(State.SIGNED_OUT));
  }
}
