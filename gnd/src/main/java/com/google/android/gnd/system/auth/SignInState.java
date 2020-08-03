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
import com.google.android.gnd.rx.ValueOrError;
import java8.util.Optional;

public class SignInState extends ValueOrError<User> {

  private final State state;

  public enum State {
    SIGNED_OUT,
    SIGNING_IN,
    SIGNED_IN,
    ERROR
  }

  SignInState(State state) {
    super(null, null);
    this.state = state;
  }

  SignInState(User user) {
    super(user, null);
    this.state = State.SIGNED_IN;
  }

  SignInState(Throwable error) {
    super(null, error);
    this.state = State.ERROR;
  }

  public State state() {
    return state;
  }

  public Optional<User> getUser() {
    return value();
  }
}