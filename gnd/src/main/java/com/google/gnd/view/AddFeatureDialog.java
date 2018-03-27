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

package com.google.gnd.view;

import android.content.Context;
import android.support.v7.app.AlertDialog;

import com.google.gnd.R;
import com.google.gnd.model.FeatureType;

import java.util.List;

import java8.util.function.Consumer;

import static java8.util.stream.StreamSupport.stream;

public class AddFeatureDialog {
  private Context context;

  public AddFeatureDialog(Context context) {
    this.context = context;
  }

  public void show(List<FeatureType> featureTypes, Consumer<FeatureType> onSelect) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(R.string.add_place_select_type_dialog_title);
    builder.setNegativeButton(R.string.add_place_cancel, (dialog, id) -> {});
    // TODO: Add icons.
    // TODO: i18n.
    String[] items =
        stream(featureTypes).map(t -> t.getListHeadingOrDefault("pt", "?")).toArray(String[]::new);
    builder.setItems(items, (dialog, idx) -> onSelect.accept(featureTypes.get(idx)));
    builder.create().show();
  }
}
