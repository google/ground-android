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

package com.google.android.gnd.ui.field;

import static java8.util.stream.StreamSupport.stream;

import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import com.google.android.gnd.R;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.MultipleChoice;
import com.google.android.gnd.model.form.Option;
import com.google.android.gnd.model.observation.MultipleChoiceResponse;
import com.google.android.gnd.model.observation.Response;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java8.util.Optional;
import java8.util.function.Consumer;

// TODO: Replace with modal bottom sheet.
public class SingleSelectDialogFactory {

  private Context context;

  SingleSelectDialogFactory(Context context) {
    this.context = context;
  }

  public AlertDialog create(
      Field field,
      Optional<Response> initialValue,
      Consumer<Optional<Response>> valueChangeCallback) {
    MultipleChoice multipleChoice = field.getMultipleChoice();
    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
    List<Option> options = multipleChoice.getOptions();
    DialogState state = new DialogState(multipleChoice, initialValue);
    dialogBuilder.setSingleChoiceItems(
        getLabels(multipleChoice), state.checkedItem, state::onSelect);
    dialogBuilder.setCancelable(false);
    dialogBuilder.setTitle(field.getLabel());
    dialogBuilder.setPositiveButton(
        R.string.apply_multiple_choice_changes,
        (dialog, which) -> valueChangeCallback.accept(state.getSelectedValue(field, options)));
    dialogBuilder.setNegativeButton(R.string.cancel, (dialog, which) -> {});
    return dialogBuilder.create();
  }

  private String[] getLabels(MultipleChoice multipleChoice) {
    return stream(multipleChoice.getOptions()).map(Option::getLabel).toArray(String[]::new);
  }

  private static class DialogState {

    private int checkedItem;

    DialogState(MultipleChoice multipleChoice, Optional<Response> initialValue) {
      // TODO: Check type.
      checkedItem =
          initialValue
              .map(MultipleChoiceResponse.class::cast)
              .flatMap(MultipleChoiceResponse::getFirstCode)
              .flatMap(multipleChoice::getIndex)
              .orElse(-1);
    }

    private void onSelect(DialogInterface dialog, int which) {
      if (checkedItem == which) {
        // Allow user to toggle values off by tapping selected item.
        checkedItem = -1;
        ((AlertDialog) dialog).getListView().setItemChecked(which, false);
      } else {
        checkedItem = which;
      }
    }

    private Optional<Response> getSelectedValue(Field field, List<Option> options) {
      if (checkedItem >= 0) {
        return Optional.of(
            new MultipleChoiceResponse(ImmutableList.of(options.get(checkedItem).getCode())));
      } else {
        return Optional.empty();
      }
    }
  }
}
