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

package com.google.android.gnd.ui.editobservation;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.gnd.databinding.BottomSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ActionBottomDialogFragment extends BottomSheetDialogFragment {

  public static final String TAG = "ActionBottomDialog";
  private AddPhotoListener listener;

  public static ActionBottomDialogFragment newInstance() {
    return new ActionBottomDialogFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    BottomSheetBinding binding = BottomSheetBinding.inflate(inflater, container, false);
    binding.setFragment(this);
    return binding.getRoot();
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    Fragment fragment = getTargetFragment();
    if (fragment instanceof AddPhotoListener) {
      listener = (AddPhotoListener) fragment;
    } else {
      Log.e(TAG, "AddPhotoListener not implemented");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    listener = null;
  }

  public void onSelectPhoto() {
    Log.d(TAG, "onSelectPhoto");
    listener.onSelectPhoto();
    dismiss();
  }

  public void onCapturePhoto() {
    Log.d(TAG, "onCapturePhoto");
    listener.onCapturePhoto();
    dismiss();
  }

  public interface AddPhotoListener {
    void onSelectPhoto();

    void onCapturePhoto();
  }
}
