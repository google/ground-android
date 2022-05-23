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

package com.google.android.gnd.ui.home.featuredetails;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.SubmissionListItemBinding;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Element.Type;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.submission.Response;
import com.google.android.gnd.model.submission.Submission;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import timber.log.Timber;

class SubmissionListItemViewHolder extends RecyclerView.ViewHolder {

  private static final int MAX_COLUMNS = 4;

  private final SubmissionListItemBinding binding;

  SubmissionListItemViewHolder(@NonNull SubmissionListItemBinding binding) {
    super(binding.getRoot());
    this.binding = binding;
  }

  public void bind(SubmissionListItemViewModel viewModel, Submission submission) {
    binding.setViewModel(viewModel);
    binding.executePendingBindings();

    // Add UI elements for each field with data.
    addFieldsFromSubmission(submission);
  }

  private void addFieldsFromSubmission(Submission submission) {
    binding.fieldLabelRow.removeAllViews();
    binding.fieldValueRow.removeAllViews();

    Form form = submission.getForm();
    // TODO: Clean this up.
    ImmutableList<Element> elements = form.getElementsSorted();
    for (int i = 0; i < MAX_COLUMNS && i < elements.size(); i++) {
      Element elem = elements.get(i);
      if (elem.getType() == Type.FIELD) {
        Field field = elem.getField();
        Optional<Response> response = submission.getResponses().getResponse(field.getId());
        binding.fieldLabelRow.addView(
            newFieldTextView(field.getLabel(), R.style.SubmissionListText_FieldLabel));
        binding.fieldValueRow.addView(
            newFieldTextView(
                response.map(Response::getSummaryText).orElse(""),
                R.style.SubmissionListText_Field));
      } else {
        Timber.e("Unhandled element type: %s", elem.getType());
      }
    }
  }

  @NonNull
  private TextView newFieldTextView(String text, @StyleRes int textAppearance) {
    Context context = binding.getRoot().getContext();
    Resources resources = context.getResources();
    TextView v = new TextView(context);
    v.setTextAppearance(context, textAppearance);
    // NOTE: These attributes don't work when applying text appearance programmatically, so we set
    // them here individually instead.
    v.setPadding(
        0, 0, resources.getDimensionPixelSize(R.dimen.submission_summary_text_padding_right), 0);
    v.setMaxWidth(resources.getDimensionPixelSize(R.dimen.submission_summary_text_max_width));
    v.setMaxLines(1);
    v.setSingleLine();
    v.setEllipsize(TextUtils.TruncateAt.END);
    v.setText(text);
    return v;
  }
}
