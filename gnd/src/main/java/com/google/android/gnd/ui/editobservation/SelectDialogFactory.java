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

package com.google.android.gnd.ui.editobservation;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import com.google.android.gnd.R;
import com.google.android.gnd.model.form.MultipleChoice;
import com.google.android.gnd.model.form.Option;
import com.google.android.gnd.model.observation.MultipleChoiceResponse;
import com.google.android.gnd.model.observation.Response;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import java8.util.function.Consumer;

public abstract class SelectDialogFactory {

  protected ImmutableList<Option> getOptions() {
    return getMultipleChoice().getOptions();
  }

  protected Option getOption(int index) {
    return getOptions().get(index);
  }

  protected int size() {
    return getOptions().size();
  }

  protected String[] getLabels() {
    return stream(getMultipleChoice().getOptions()).map(Option::getLabel).toArray(String[]::new);
  }

  /** Returns the selected item(s) as a {@link Response}. */
  protected Optional<Response> onCreateResponse() {
    return MultipleChoiceResponse.fromList(
        stream(getSelectedOptions()).map(Option::getId).collect(toImmutableList()));
  }

  // TODO: Replace with modal bottom sheet.
  protected AlertDialog.Builder createDialogBuilder() {
    return new AlertDialog.Builder(getContext())
        .setCancelable(false)
        .setTitle(getTitle())
        .setPositiveButton(
            R.string.apply_multiple_choice_changes,
            (dialog, which) -> getValueConsumer().accept(onCreateResponse()))
        .setNegativeButton(R.string.cancel, (dialog, which) -> {});
  }

  public AlertDialog createDialog() {
    initSelectedState();
    return createDialogBuilder().create();
  }

  /** Creates and displays the dialog. */
  protected void show() {
    createDialog().show();
  }

  /** Initialize current state. */
  protected abstract void initSelectedState();

  /** List of selected options. */
  protected abstract ImmutableList<Option> getSelectedOptions();

  public abstract Context getContext();

  public abstract String getTitle();

  public abstract MultipleChoice getMultipleChoice();

  public abstract Optional<MultipleChoiceResponse> getCurrentValue();

  public abstract Consumer<Optional<Response>> getValueConsumer();

  public abstract static class Builder<B> {

    public abstract B setContext(Context context);

    public abstract B setTitle(String title);

    public abstract B setMultipleChoice(MultipleChoice multipleChoice);

    public abstract B setCurrentValue(Optional<MultipleChoiceResponse> response);

    public abstract B setValueConsumer(Consumer<Optional<Response>> consumer);
  }
}
