/*
 * Copyright 2021 Google LLC
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

import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.system.auth.SignInState.State;
import io.reactivex.disposables.Disposable;
import javax.inject.Inject;

/** Manages ToS acceptance state. */
public class TermsOfServiceManager {
  private final LocalValueStore localValueStore;
  private final Disposable signInStateSubscription;

  // TODO: Inject into activity.
  @Inject
  public TermsOfServiceManager(
      LocalValueStore localValueStore, AuthenticationManager authenticationManager) {
    this.localValueStore = localValueStore;

    signInStateSubscription =
        authenticationManager.getSignInState().subscribe(this::onSignInStateChange);
  }

  private void onSignInStateChange(SignInState signInState) {
    if (signInState.state() != State.SIGNED_IN) {
      localValueStore.setTermsAccepted(false);
    }
  }

  @Override
  protected void finalize() throws Throwable {
    if (signInStateSubscription != null) {
      signInStateSubscription.dispose();
    }
    super.finalize();
  }
}
