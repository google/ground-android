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

package com.google.android.gnd.ui.editobservation;

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java.util.Objects.requireNonNull;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.gnd.model.form.Option;
import com.google.android.gnd.model.observation.MultipleChoiceResponse;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java8.util.stream.IntStreams;

@AutoValue
abstract class MultiSelectDialogFactory extends SelectDialogFactory {

  @Nullable private boolean[] checkedItems;

  public static Builder builder() {
    return new AutoValue_MultiSelectDialogFactory.Builder();
  }

  @Override
  protected ImmutableList<Option> getSelectedOptions() {
    return IntStreams.range(0, size())
        .filter(value -> requireNonNull(checkedItems)[value])
        .boxed()
        .map(this::getOption)
        .collect(toImmutableList());
  }

  @Override
  protected AlertDialog.Builder createDialogBuilder() {
    return super.createDialogBuilder()
        .setMultiChoiceItems(getLabels(), checkedItems, (dialog, which, isChecked) -> {});
  }

  @Override
  public void initSelectedState() {
    checkedItems = new boolean[size()];
    getCurrentValue().ifPresent(this::updateCurrentSelectedItems);
  }

  private void updateCurrentSelectedItems(MultipleChoiceResponse response) {
    IntStreams.range(0, size())
        .forEach(i -> requireNonNull(checkedItems)[i] = response.isSelected(getOption(i)));
  }

  @AutoValue.Builder
  public abstract static class Builder extends SelectDialogFactory.Builder<Builder> {
    public abstract MultiSelectDialogFactory build();
  }
}
