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

package com.google.android.gnd.ui.sheet.input;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gnd.R;
import com.google.android.gnd.model.FeatureUpdate;
import com.google.android.gnd.model.Form.MultipleChoice;
import com.google.android.gnd.model.Record;
import com.google.android.gnd.model.Record.Choices;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import java8.util.Optional;
import java8.util.stream.Collectors;

import static com.google.android.gnd.model.FeatureUpdate.Operation.CREATE;
import static com.google.android.gnd.model.FeatureUpdate.Operation.DELETE;
import static com.google.android.gnd.model.FeatureUpdate.Operation.NO_CHANGE;
import static com.google.android.gnd.model.FeatureUpdate.Operation.UPDATE;
import static com.google.android.gnd.ui.util.ViewUtil.getColorForStates;
import static java8.util.stream.StreamSupport.stream;

public class MultipleChoiceFieldView extends ConstraintLayout implements Editable {
  private static final String TAG = MultipleChoiceFieldView.class.getSimpleName();
  private String elementId;
  private Optional<Record.Value> originalValue;
  private Optional<Record.Value> currentValue;
  private String label;
  private int defaultLabelColor;
  private int focusedLabelColor;
  private MultipleChoice multipleChoice;
  // TODO: Refactor dialogs into their own classes.
  private int selectedItem = -1;
  private boolean required;

  @BindView(R.id.multiple_choice_field_label)
  TextView labelText;

  @BindView(R.id.multiple_choice_field_value)
  TextView valueText;

  @BindView(R.id.error_text)
  TextView errorMessageTextView;

  public MultipleChoiceFieldView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    ButterKnife.bind(this);

    focusedLabelColor = getColorForStates(labelText, TextView.FOCUSED_STATE_SET);
    defaultLabelColor = getColorForStates(labelText, TextView.ENABLED_STATE_SET);
  }

  @OnClick(R.id.multiple_choice_dropdown_btn)
  void onClick(View view) {
    if (!valueText.isEnabled()) {
      return;
    }
    if (valueText.isFocused()) {
      showDialog();
    } else {
      valueText.requestFocus();
    }
  }

  public void init(
      String elementId, String label, Optional<Record.Value> value, MultipleChoice mc, boolean
      required) {
    this.elementId = elementId;
    originalValue = value;
    labelText.setText(label);
    this.label = label;
    this.multipleChoice = mc;
    this.required = required;
    onValueUpdate(value);
  }

  @OnFocusChange(R.id.multiple_choice_field_value)
  void onValueTextFocusChange(View view, boolean hasFocus) {
    if (!valueText.isEnabled()) {
      return;
    }
    if (hasFocus) {
      showDialog();
      labelText.setTextColor(focusedLabelColor);
    } else {
      labelText.setTextColor(defaultLabelColor);
    }
    updateValidationMessage();
  }

  @OnClick(R.id.multiple_choice_field_value)
  void showDialog() {
    switch (multipleChoice.getCardinality()) {
      case SELECT_MULTIPLE:
        showMultipleChoiceDialog();
        break;
      case SELECT_ONE:
        showSingleChoiceDialog();
        break;
      default:
        Log.e(TAG, "Unknown cardinality: " + multipleChoice.getCardinality());
        return;
    }
  }

  private void showMultipleChoiceDialog() {
    if (!valueText.isEnabled()) {
      return;
    }
    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
    List<MultipleChoice.Option> options = multipleChoice.getOptionsList();
    String[] labels = new String[options.size()];
    boolean[] values = new boolean[options.size()];
    for (int i = 0; i < options.size(); i++) {
      // TODO: i18n.
      labels[i] = options.get(i).getLabelsOrDefault("pt", "?");
      values[i] = isOptionSelected(options.get(i));
    }
    dialogBuilder.setMultiChoiceItems(labels, values, (dialog, which, isChecked) -> {});

    dialogBuilder.setCancelable(false);
    // TODO: i18n; what happens when user changes device language and label is stored here?
    dialogBuilder.setTitle(label);
    dialogBuilder.setPositiveButton(
        R.string.apply_multiple_choice_changes,
        (dialog, which) -> {
          Choices.Builder choices = Choices.newBuilder();
          for (int i = 0; i < options.size(); i++) {
            if (values[i]) {
              choices.addCodes(options.get(i).getCode());
            }
          }
          onValueUpdate(
              choices.getCodesCount() == 0
                  ? Optional.empty()
                  : Optional.of(Record.Value.newBuilder().setChoices(choices).build()));
          valueText.requestFocus();
          updateValidationMessage();

        });
    dialogBuilder.setNegativeButton(R.string.discard_multiple_choice_changes, (dialog, which) -> {});
    dialogBuilder.create().show();
  }

  private void showSingleChoiceDialog() {
    if (!valueText.isEnabled()) {
      return;
    }
    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
    List<MultipleChoice.Option> options = multipleChoice.getOptionsList();
    String[] labels = new String[options.size()];
    selectedItem = -1;
    for (int i = 0; i < options.size(); i++) {
      // TODO: i18n.
      labels[i] = options.get(i).getLabelsOrDefault("pt", "?");
      if (isOptionSelected(options.get(i))) {
        selectedItem = i;
      }
    }
    dialogBuilder.setSingleChoiceItems(
        labels,
        selectedItem,
        (dialog, which) -> {
          if (selectedItem == which) {
            // Allow user to toggle values off by tapping selected item.
            selectedItem = -1;
            ((AlertDialog) dialog).getListView().setItemChecked(which, false);
          } else {
            selectedItem = which;
          }
        });

    dialogBuilder.setCancelable(false);
    // TODO: i18n; what happens when user changes device language and label is stored here?
    dialogBuilder.setTitle(label);
    dialogBuilder.setPositiveButton(
        R.string.apply_multiple_choice_changes,
        (dialog, which) -> {
          Choices.Builder choices = Choices.newBuilder();
          if (selectedItem >= 0) {
            choices.addCodes(options.get(selectedItem).getCode());
          }
          onValueUpdate(
              choices.getCodesCount() == 0
                  ? Optional.empty()
                  : Optional.of(Record.Value.newBuilder().setChoices(choices).build()));
          valueText.requestFocus();
          updateValidationMessage();
        });
    dialogBuilder.setNegativeButton(R.string.discard_multiple_choice_changes, (dialog, which) -> {});
    dialogBuilder.create().show();
  }

  private boolean isOptionSelected(MultipleChoice.Option option) {
    return currentValue.isPresent()
        && currentValue.get().getChoices().getCodesList().contains(option.getCode());
  }

  private void onValueUpdate(Optional<Record.Value> value) {
    currentValue = value;
    if (!value.isPresent()) {
      valueText.setText("");
      return;
    }
    valueText.setText(
        stream(value.get().getChoices().getCodesList())
            .map(this::getOptionLabel)
            .collect(Collectors.joining(", ")));
  }

  private String getOptionLabel(String code) {
    // TODO: i18n.
    return stream(multipleChoice.getOptionsList())
        .filter(o -> o.getCode().equals(code))
        .map(o -> o.getLabelsOrDefault("pt", "?"))
        .findFirst()
        .orElse("?");
  }

  private Optional<Record.Value> getCurrentValue() {
    return currentValue;
  }

  @NonNull
  private String getText() {
    return valueText.getText().toString().trim();
  }

  @Override
  public boolean isModified() {
    return !getText().equals(originalValue);
  }

  @Override
  public FeatureUpdate.RecordUpdate.ValueUpdate getUpdate() {
    FeatureUpdate.RecordUpdate.ValueUpdate.Builder update =
        FeatureUpdate.RecordUpdate.ValueUpdate.newBuilder();
    update.setElementId(elementId);
    Optional<Record.Value> currentValue = getCurrentValue();
    if (currentValue.equals(originalValue)) {
      update.setOperation(NO_CHANGE);
    } else if (!currentValue.isPresent()) {
      update.setOperation(DELETE);
    } else if (originalValue.isPresent()) {
      update.setOperation(UPDATE);
      update.setValue(currentValue.get());
    } else {
      update.setOperation(CREATE);
      update.setValue(currentValue.get());
    }
    return update.build();
  }

  @Override
  public void setMode(Mode mode) {
    valueText.setEnabled(mode == Mode.EDIT);
  }


  @Override
  public boolean isValid() {
    return !isMissing();
  }


  boolean isMissing() {
    return required && !getCurrentValue().isPresent();
  }

  @Override
  public void updateValidationMessage() {
    if (isMissing()) {
      errorMessageTextView.setText(R.string.required_field);
      errorMessageTextView.setVisibility(VISIBLE);
    } else {
      errorMessageTextView.setVisibility(GONE);
    }
  }

  @Override
  public void setFocus() {
    this.requestFocus();
//    ViewUtil.showSoftInputMode((Activity) getContext()); // TODO: Why doesn't this work?
  }

}
