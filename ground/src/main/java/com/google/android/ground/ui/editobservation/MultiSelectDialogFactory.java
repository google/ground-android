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

package com.google.android.ground.ui.editobservation;

import static java8.util.stream.StreamSupport.stream;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import com.google.android.ground.R;
import com.google.android.ground.model.form.Field;
import com.google.android.ground.model.form.MultipleChoice;
import com.google.android.ground.model.form.Option;
import com.google.android.ground.model.observation.MultipleChoiceResponse;
import com.google.android.ground.model.observation.Response;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.stream.IntStreams;

// TODO: Replace with modal bottom sheet.
class MultiSelectDialogFactory {

  private Context context;

  MultiSelectDialogFactory(Context context) {
    this.context = context;
  }

  AlertDialog create(
      Field field,
      Optional<Response> initialResponse,
      Consumer<Optional<Response>> responseChangeCallback) {
    MultipleChoice multipleChoice = field.getMultipleChoice();
    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
    List<Option> options = multipleChoice.getOptions();
    final DialogState state = new DialogState(multipleChoice, initialResponse);
    dialogBuilder.setMultiChoiceItems(
        getLabels(multipleChoice), state.checkedItems, (dialog, which, isChecked) -> {});
    dialogBuilder.setCancelable(false);
    dialogBuilder.setTitle(field.getLabel());
    dialogBuilder.setPositiveButton(
        R.string.apply_multiple_choice_changes,
        (dialog, which) -> responseChangeCallback.accept(state.getSelectedValues(options)));
    dialogBuilder.setNegativeButton(
        R.string.discard_multiple_choice_changes, (dialog, which) -> {});
    return dialogBuilder.create();
  }

  private String[] getLabels(MultipleChoice multipleChoice) {
    return stream(multipleChoice.getOptions()).map(Option::getLabel).toArray(String[]::new);
  }

  private static class DialogState {

    private boolean[] checkedItems;

    public DialogState(MultipleChoice multipleChoice, Optional<Response> initialResponse) {
      ImmutableList<Option> options = multipleChoice.getOptions();
      checkedItems = new boolean[options.size()];
      // TODO: Check cast.
      initialResponse.ifPresent(
          r ->
              IntStreams.range(0, options.size())
                  .forEach(
                      i ->
                          checkedItems[i] =
                              ((MultipleChoiceResponse) r).isSelected(options.get(i))));
    }

    private Optional<Response> getSelectedValues(List<Option> options) {
      ImmutableList.Builder<String> choices = new ImmutableList.Builder<>();
      for (int i = 0; i < options.size(); i++) {
        if (checkedItems[i]) {
          choices.add(options.get(i).getCode());
        }
      }
      return MultipleChoiceResponse.fromList(choices.build());
    }
  }
}
