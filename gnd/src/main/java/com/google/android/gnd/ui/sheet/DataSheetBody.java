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

package com.google.android.gnd.ui.sheet;

import static com.google.android.gnd.model.PlaceUpdate.Operation.NO_CHANGE;
import static com.google.android.gnd.ui.util.ViewUtil.children;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.google.android.gnd.model.PlaceUpdate.RecordUpdate;
import com.google.android.gnd.ui.sheet.input.Editable;
import java.util.List;
import java8.util.stream.Collectors;

public class DataSheetBody extends LinearLayout {
  private boolean saved;

  // TODO: Update once there can only be one sheet edited at a time.

  public DataSheetBody(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public void clear() {
    saved = false;
    this.removeAllViews();
  }

  public List<RecordUpdate> getUpdates() {
    return children(this)
        .filter(RecordView.class::isInstance)
        .map(RecordView.class::cast)
        .map(RecordView::getUpdates)
        .filter(u -> u.getOperation() != NO_CHANGE || u.getValueUpdatesCount() > 0)
        .collect(Collectors.toList());
  }

  public void updateValidationMessages() {
    children(this)
        .filter(RecordView.class::isInstance)
        .map(RecordView.class::cast)
        .forEach(r -> r.updateValidationMessages());
  }

  public boolean isValid() {
    return children(this)
        .filter(RecordView.class::isInstance)
        .map(RecordView.class::cast)
        .filter(v -> v.getMode() == Editable.Mode.EDIT)
        .allMatch(v -> v.isValid());
  }

  public boolean isSaved() {
    return saved;
  }

  public void markAsSaved() {
    saved = true;
  }
}
