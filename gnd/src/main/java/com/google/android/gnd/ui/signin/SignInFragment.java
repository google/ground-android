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
import androidx.annotation.Nullable;
import com.google.android.gnd.databinding.SignInFragBinding;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BackPressListener;
import dagger.hilt.android.AndroidEntryPoint;

// TODO: Remove scoping since Fragments are no longer injected.
@AndroidEntryPoint
public class SignInFragment extends AbstractFragment implements BackPressListener {
  private SignInViewModel viewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.viewModel = getViewModel(SignInViewModel.class);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    SignInFragBinding binding = SignInFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public boolean onBack() {
    // Workaround to exit on back from sign-in screen since for some reason
    // popUpTo is not working on signOut action.
    getActivity().finish();
    return false;
  }
}
