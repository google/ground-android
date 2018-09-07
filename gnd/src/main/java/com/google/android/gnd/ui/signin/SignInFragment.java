/*
 * Copyright 2018 Google LLC
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

package com.google.android.gnd.ui.signin;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.android.gnd.R;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.system.AuthenticationManager.AuthStatus;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import javax.inject.Inject;

public class SignInFragment extends AbstractFragment {

  private static final String TAG = SignInFragment.class.getSimpleName();

  @Inject
  AuthenticationManager authenticationManager;

  @BindView(R.id.sign_in_button)
  View signInButton;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.sign_in_frag, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    authenticationManager
      .getAuthStatus()
      .as(autoDisposable(this))
      .subscribe(this::onAuthStatusChange);
  }

  @OnClick(R.id.sign_in_button)
  public void onSignInButtonClick() {
    // TODO: Show spinner.
    authenticationManager.signIn(getActivity());
  }

  public void onAuthStatusChange(AuthStatus status) {
    switch (status.getState()) {
      case SIGNED_OUT:
        // TODO: Hide spinner.
        break;
      case SIGNING_IN:
        // TODO: Show spinner.
        break;
      case SIGNED_IN:
        getNavController().navigate(SignInFragmentDirections.proceedToHomeScreen());
        break;
      case ERROR:
        Log.d(TAG, "Authentication error", status.getError());
        EphemeralPopups.showError(getContext(), R.string.sign_in_unsuccessful);
        break;
    }
  }
}
