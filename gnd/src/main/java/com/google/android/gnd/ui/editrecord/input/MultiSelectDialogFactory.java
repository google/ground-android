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
package com.google.android.gnd.ui.editrecord.input;

import static java8.util.stream.StreamSupport.stream;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import com.google.android.gnd.R;
import com.google.android.gnd.vo.Form.Field;
import com.google.android.gnd.vo.Form.MultipleChoice;
import com.google.android.gnd.vo.Form.MultipleChoice.Option;
import com.google.android.gnd.vo.Record;
import com.google.android.gnd.vo.Record.Value;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.stream.IntStreams;

class MultiSelectDialogFactory {
  private Context context;

  MultiSelectDialogFactory(Context context) {
    this.context = context;
  }

  AlertDialog create(
    Field field, Optional<Value> initialValue, Consumer<Optional<Value>> valueChangeCallback) {
    MultipleChoice multipleChoice = field.getMultipleChoice();
    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
    List<Option> options = multipleChoice.getOptions();
    final DialogState state = new DialogState(multipleChoice, initialValue);
    dialogBuilder.setMultiChoiceItems(
      getLabels(multipleChoice), state.checkedItems, (dialog, which, isChecked) -> {
      });
    dialogBuilder.setCancelable(false);
    dialogBuilder.setTitle(field.getLabel());
    dialogBuilder.setPositiveButton(
      R.string.apply_multiple_choice_changes,
      (dialog, which) -> valueChangeCallback.accept(state.getSelectedValues(options)));
    dialogBuilder.setNegativeButton(
      R.string.discard_multiple_choice_changes, (dialog, which) -> {
      });
    return dialogBuilder.create();
  }

  private String[] getLabels(MultipleChoice multipleChoice) {
    return stream(multipleChoice.getOptions()).map(Option::getLabel).toArray(String[]::new);
  }

  private static class DialogState {
    private boolean[] checkedItems;

    public DialogState(MultipleChoice multipleChoice, Optional<Value> initialValue) {
      ImmutableList<Option> options = multipleChoice.getOptions();
      checkedItems = new boolean[options.size()];
      initialValue.ifPresent(
        v ->
          IntStreams.range(0, options.size())
                    .forEach(i -> checkedItems[i] = v.isSelected(options.get(i))));
    }

    private Optional<Value> getSelectedValues(List<Option> options) {
      Record.Choices.Builder choices = Record.Choices.newBuilder();
      for (int i = 0; i < options.size(); i++) {
        if (checkedItems[i]) {
          choices.codesBuilder().add(options.get(i).getCode());
        }
      }
      return Optional.of(choices.build())
                     .filter(ch -> !ch.getCodes().isEmpty())
                     .map(Record.Value::ofChoices);
    }
  }
}
