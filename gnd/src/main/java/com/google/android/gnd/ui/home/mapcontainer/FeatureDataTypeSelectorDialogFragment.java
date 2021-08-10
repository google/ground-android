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

package com.google.android.gnd.ui.home.mapcontainer;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.Nullable;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.common.AbstractDialogFragment;
import dagger.hilt.android.AndroidEntryPoint;
import java8.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

@AndroidEntryPoint
public class FeatureDataTypeSelectorDialogFragment extends AbstractDialogFragment {

  private final Consumer<Integer> onSelectFeatureDataType;

  public FeatureDataTypeSelectorDialogFragment(Consumer<Integer> onSelectFeatureDataType) {
    this.onSelectFeatureDataType = onSelectFeatureDataType;
  }

  @NotNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);
    ArrayAdapter<String> listAdapter =
        new ArrayAdapter<>(getContext(), R.layout.project_selector_list_item, R.id.project_name);
    listAdapter.add(getString(R.string.point));
    listAdapter.add(getString(R.string.polygon));
    return new Builder(getContext())
        .setTitle(R.string.select_feature_type)
        .setAdapter(
            listAdapter,
            (dialog, position) -> {
              onSelectFeatureDataType.accept(position);
            })
        .setCancelable(true)
        .create();
  }
}
