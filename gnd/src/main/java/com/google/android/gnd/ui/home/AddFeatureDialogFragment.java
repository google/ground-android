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

package com.google.android.gnd.ui.home;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import com.google.android.gnd.R;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.ui.common.AbstractDialogFragment;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.AndroidEntryPoint;
import java8.util.Objects;
import javax.inject.Inject;
import timber.log.Timber;

@AndroidEntryPoint
public class AddFeatureDialogFragment extends AbstractDialogFragment {

  private static final String TAG = AddFeatureDialogFragment.class.getSimpleName();

  @Nullable private Project project;
  @Nullable private LayerSelectedListener listener;

  @Inject
  public AddFeatureDialogFragment() {}

  public void show(
      @NonNull Project project,
      @NonNull FragmentManager fragmentManager,
      @NonNull LayerSelectedListener listener) {
    this.project = project;
    this.listener = listener;
    show(fragmentManager, TAG);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);
    try {
      return createDialog(Objects.requireNonNull(project));
    } catch (RuntimeException e) {
      Timber.e(e);
      return fail(Objects.requireNonNullElse(e.getMessage(), "Unknown error"));
    }
  }

  private Dialog createDialog(Project project) {
    ImmutableList<Layer> layers =
        stream(project.getLayers())
            .sorted((pt1, pt2) -> pt1.getName().compareTo(pt2.getName()))
            .collect(toImmutableList());
    String[] items = stream(layers).map(Layer::getName).toArray(String[]::new);

    // TODO: Add icons.
    return new AlertDialog.Builder(requireContext())
        .setTitle(R.string.add_feature_select_type_dialog_title)
        .setNegativeButton(R.string.cancel, (dialog, id) -> dismiss())
        .setItems(items, (dialog, idx) -> onSelectLayer(layers.get(idx)))
        .create();
  }

  private void onSelectLayer(Layer layer) {
    if (listener != null) {
      listener.onSelected(layer);
    }
  }

  public interface LayerSelectedListener {
    void onSelected(Layer layer);
  }
}
