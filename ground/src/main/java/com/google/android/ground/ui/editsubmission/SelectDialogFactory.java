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

package com.google.android.ground.ui.editsubmission;

import static java8.util.stream.StreamSupport.stream;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import com.google.android.ground.R;
import com.google.android.ground.model.submission.MultipleChoiceTaskData;
import com.google.android.ground.model.task.MultipleChoice;
import com.google.android.ground.model.task.Option;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import java8.util.function.Consumer;

public abstract class SelectDialogFactory {

  private kotlinx.collections.immutable.ImmutableList<Option> getOptions() {
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

  // TODO: Replace with modal bottom sheet.
  protected AlertDialog.Builder createDialogBuilder() {
    return new AlertDialog.Builder(getContext())
        .setCancelable(false)
        .setTitle(getTitle())
        .setPositiveButton(
            R.string.apply_multiple_choice_changes,
            (dialog, which) -> getValueConsumer().accept(getSelectedOptions()))
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

  public abstract Optional<MultipleChoiceTaskData> getCurrentResponse();

  public abstract Consumer<ImmutableList<Option>> getValueConsumer();

  public abstract static class Builder<B> {

    public abstract B setContext(Context context);

    public abstract B setTitle(String title);

    public abstract B setMultipleChoice(MultipleChoice multipleChoice);

    public abstract B setCurrentResponse(Optional<MultipleChoiceTaskData> response);

    public abstract B setValueConsumer(Consumer<ImmutableList<Option>> consumer);
  }
}
