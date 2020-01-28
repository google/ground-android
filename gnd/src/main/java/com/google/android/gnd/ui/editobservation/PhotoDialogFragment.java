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
import com.google.android.gnd.databinding.EditObservationBottomSheetBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Generates a bottom sheet with options for adding a photo to the observation.
 *
 * <p>Since one observation can have multiple photo fields, hence it is must to map the {@link
 * Field#getId() along with each request.}
 */
public class PhotoDialogFragment extends BottomSheetDialogFragment {

  public static final String TAG = PhotoDialogFragment.class.getSimpleName();
  private static final String FIELD_ID_BUNDLE_ARG = "field_id";
  private AddPhotoListener listener;

  @Nullable private String fieldId;

  public static PhotoDialogFragment newInstance(String fieldId) {
    Bundle bundle = new Bundle();
    bundle.putString(FIELD_ID_BUNDLE_ARG, fieldId);
    PhotoDialogFragment fragment = new PhotoDialogFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      fieldId = getArguments().getString(FIELD_ID_BUNDLE_ARG);
    }
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    EditObservationBottomSheetBinding binding =
        EditObservationBottomSheetBinding.inflate(inflater, container, false);
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
    listener.onSelectPhoto(fieldId);
    dismiss();
  }

  public void onCapturePhoto() {
    listener.onCapturePhoto(fieldId);
    dismiss();
  }

  public interface AddPhotoListener {
    void onSelectPhoto(String fieldId);

    void onCapturePhoto(String fieldId);
  }
}
