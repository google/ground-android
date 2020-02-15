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

package com.google.android.gnd.ui.observationdetails;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.gnd.R;

class FieldViewHolder {

  private ViewGroup root;

  @BindView(R.id.field_label)
  TextView labelView;

  @BindView(R.id.field_value)
  TextView valueView;

  private FieldViewHolder(ViewGroup root) {
    this.root = root;
  }

  static FieldViewHolder newInstance(LayoutInflater inflater) {
    ViewGroup root = (ViewGroup) inflater.inflate(R.layout.observation_details_field, null);
    FieldViewHolder holder = new FieldViewHolder(root);
    ButterKnife.bind(holder, root);
    return holder;
  }

  void setLabel(String label) {
    labelView.setText(label);
  }

  void setValue(String value) {
    valueView.setText(value);
  }

  public ViewGroup getRoot() {
    return root;
  }
}
