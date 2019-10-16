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

package com.google.android.gnd.ui.home.featuresheet;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gnd.R;
import com.google.android.gnd.databinding.RecordListItemBinding;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.Record;
import com.google.android.gnd.model.observation.RecordWrapper;
import com.google.android.gnd.model.observation.Response;

import java8.util.Optional;

class RecordListItemViewHolder extends RecyclerView.ViewHolder {

  private static final int MAX_SUMMARY_COLUMNS = 4;

  private final EventHandler handler;
  private final RecordListItemBinding binding;

  RecordListItemViewHolder(@NonNull RecordListItemBinding binding, @NonNull EventHandler handler) {
    super(binding.getRoot());
    this.binding = binding;
    this.handler = handler;
  }

  public void bind(Record record) {
    binding.setRecordWrapper(new RecordWrapper(record));
    binding.setHandler(handler);
    binding.executePendingBindings();

    // add label/value for each field
    addFieldsFromRecord(record);
  }

  private void addFieldsFromRecord(Record record) {
    binding.fieldLabelRow.removeAllViews();
    binding.fieldValueRow.removeAllViews();

    Form form = record.getForm();
    // TODO: Clean this up.
    for (int i = 0; i < MAX_SUMMARY_COLUMNS && i < form.getElements().size(); i++) {
      Element elem = form.getElements().get(i);
      switch (elem.getType()) {
        case FIELD:
          Field field = elem.getField();
          Optional<Response> response = record.getResponses().getResponse(field.getId());
          binding.fieldLabelRow.addView(
                  newFieldTextView(field.getLabel(), R.style.RecordListText_FieldLabel));
          binding.fieldValueRow.addView(
                  newFieldTextView(
                          response.map(r -> r.getSummaryText(field)).orElse(""),
                          R.style.RecordListText_Field));
          break;
      }
    }
  }

  @NonNull
  private TextView newFieldTextView(String text, int textAppearance) {
    Context context = binding.getRoot().getContext();
    Resources resources = context.getResources();
    TextView v = new TextView(context);
    v.setTextAppearance(context, textAppearance);
    // NOTE: These attributes don't work when applying text appearance programmatically, so we set
    // them here individually instead.
    v.setPadding(
            0, 0, resources.getDimensionPixelSize(R.dimen.record_summary_text_padding_right), 0);
    v.setMaxWidth(resources.getDimensionPixelSize(R.dimen.record_summary_text_max_width));
    v.setMaxLines(1);
    v.setSingleLine();
    v.setEllipsize(TextUtils.TruncateAt.END);
    v.setText(text);
    return v;
  }
}
