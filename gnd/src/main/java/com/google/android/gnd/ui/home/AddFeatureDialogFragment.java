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
import static java.util.Objects.requireNonNull;
import static java8.util.stream.StreamSupport.stream;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import androidx.fragment.app.FragmentManager;
import com.google.android.gnd.R;
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

  @Nullable private Consumer<Layer> layerConsumer;
  @Nullable private ImmutableList<Layer> layers;

  @Inject
  public AddFeatureDialogFragment() {}

  public void show(
      ImmutableList<Layer> layers, FragmentManager fragmentManager, Consumer<Layer> layerConsumer) {
    this.layers = layers;
    this.layerConsumer = layerConsumer;
    show(fragmentManager, TAG);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);
    try {
      requireNonNull(layers);
      requireNonNull(layerConsumer);
      return createDialog(sortByName(layers), layerConsumer);
    } catch (RuntimeException e) {
      Timber.e(e);
      return fail(Objects.requireNonNullElse(e.getMessage(), "Unknown error"));
    }
  }

  private ImmutableList<Layer> sortByName(ImmutableList<Layer> layers) {
    return stream(layers)
        .sorted((l1, l2) -> l1.getName().compareTo(l2.getName()))
        .collect(toImmutableList());
  }

  private String[] getLayerNames(ImmutableList<Layer> layers) {
    return stream(layers).map(Layer::getName).toArray(String[]::new);
  }

  private Dialog createDialog(ImmutableList<Layer> layers, Consumer<Layer> layerConsumer) {
    // TODO: Add icons.
    return new AlertDialog.Builder(requireContext())
        .setTitle(R.string.add_feature_select_type_dialog_title)
        .setNegativeButton(R.string.cancel, (dialog, id) -> dismiss())
        .setItems(getLayerNames(layers), (dialog, index) -> layerConsumer.accept(layers.get(index)))
        .create();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    layers = null;
    layerConsumer = null;
  }
}
