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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.android.gnd.R;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BackPressListener;
import javax.inject.Inject;

@ActivityScoped
public class SignInFragment extends AbstractFragment implements BackPressListener {
  @Inject AuthenticationManager authenticationManager;

  @BindView(R.id.sign_in_button)
  View signInButton;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.sign_in_frag, container, false);
  }

  @OnClick(R.id.sign_in_button)
  public void onSignInButtonClick() {
    authenticationManager.signIn();
  }

  @Override
  public boolean onBack() {
    // Workaround to exit on back from sign-in screen since for some reason
    // popUpTo is not working on signOut action.
    getActivity().finish();
    return false;
  }
}
