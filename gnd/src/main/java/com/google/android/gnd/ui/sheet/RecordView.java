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

import static com.google.android.gnd.model.PlaceUpdate.Operation.CREATE;
import static com.google.android.gnd.model.PlaceUpdate.Operation.NO_CHANGE;
import static com.google.android.gnd.model.PlaceUpdate.Operation.UPDATE;
import static com.google.android.gnd.ui.sheet.input.Editable.Mode.EDIT;
import static com.google.android.gnd.ui.util.ViewUtil.children;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.gnd.R;
import com.google.android.gnd.model.Form;
import com.google.android.gnd.model.Form.Element;
import com.google.android.gnd.model.Form.Field;
import com.google.android.gnd.model.Form.MultipleChoice;
import com.google.android.gnd.model.Form.TextField;
import com.google.android.gnd.model.PlaceUpdate.RecordUpdate;
import com.google.android.gnd.model.PlaceUpdate.RecordUpdate.ValueUpdate;
import com.google.android.gnd.model.Record;
import com.google.android.gnd.model.Record.Value;
import com.google.android.gnd.model.Timestamps;
import com.google.android.gnd.ui.sheet.input.Editable;
import com.google.android.gnd.ui.sheet.input.Editable.Mode;
import com.google.android.gnd.ui.sheet.input.MultipleChoiceFieldView;
import com.google.android.gnd.ui.sheet.input.TextFieldView;
import com.google.protobuf.Timestamp;
import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.stream.Stream;

public class RecordView extends FrameLayout {

  private static final String TAG = RecordView.class.getSimpleName();
  // TODO: Refactor me!
  private static final long MILLIS_PER_SECOND = 1000;
  private static final long NANOS_PER_MILLISECOND = 1000000;
  private final LayoutInflater inflater;

  @BindView(R.id.record_view_contents)
  ViewGroup contents;

  @BindView(R.id.record_menu_btn)
  ImageView menuBtn;

  @BindView(R.id.record_heading)
  TextView recordHeading;

  @BindView(R.id.record_heading_container)
  ViewGroup headingContainer;

  private Record record;
  private Editable.Mode mode;

  /** Map from elementId to Response. */
  public RecordView(Context context, Consumer<RecordView> onEditRecordClick) {
    super(context);
    inflater = LayoutInflater.from(context);
    inflate(context, R.layout.record_details_view, this);
    ButterKnife.bind(this);
    setUpPopupMenu(onEditRecordClick);
  }

  public static long toMillis(Timestamp timestamp) {
    return (timestamp.getSeconds() * MILLIS_PER_SECOND)
        + (timestamp.getNanos() / NANOS_PER_MILLISECOND);
  }

  private void setUpPopupMenu(Consumer<RecordView> onEditRecordClick) {
    PopupMenu popupMenu = new PopupMenu(getContext(), menuBtn);
    // TODO: Wrap inflate and click handlers in custom view.
    menuBtn.setOnClickListener((v) -> popupMenu.show());
    popupMenu.inflate(R.menu.record_menu);
    popupMenu.setOnMenuItemClickListener(
        menuItem -> {
          switch (menuItem.getItemId()) {
            case R.id.edit_record_menu_item:
              onEditRecordClick.accept(this);
              break;
          }
          return true;
        });
  }

  public void populate(Form form, Record record, Editable.Mode mode) {
    headingContainer.setVisibility(mode == Mode.EDIT ? GONE : VISIBLE);
    this.record = record;
    contents.removeAllViews();
    recordHeading.setText(getRecordHeadingText());
    for (Form.Element e : form.getElementsList()) {
      addFormElement(e);
    }
    setMode(mode);
  }

  private String getRecordHeadingText() {
    if (record.getServerTimestamps().getModified().getSeconds() == 0) {
      return "Saved "
          + getRelativeTimeSpan(record.getClientTimestamps().getModified())
          + ". Sync pending.";
    }
    Timestamps serverTimestamps = record.getServerTimestamps();
    if (serverTimestamps.getCreated().equals(serverTimestamps.getModified())) {
      return String.format(
          getResources().getString(R.string.submitted_status),
          getRelativeTimeSpan(serverTimestamps.getCreated()));
    } else {
      return String.format(
          getResources().getString(R.string.updated_status),
          getRelativeTimeSpan(serverTimestamps.getCreated()));
    }
  }

  private CharSequence getRelativeTimeSpan(Timestamp timestamp) {
    long time = toMillis(timestamp);
    long now = System.currentTimeMillis();
    return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS)
        .toString()
        .toLowerCase();
  }

  // TODO: Refactor into "FormBuilder" class, used by this class to update form based on stream.
  private void addFormElement(Element e) {
    switch (e.getElementTypeCase()) {
      case FIELD:
        addFormField(e.getId(), e.getField());
        break;
      default:
        Log.d(TAG, "Skipping unsupported form element type: " + e.getElementTypeCase());
    }
  }

  private void addFormField(String elementId, Field f) {
    String label = f.getLabelOrDefault("pt", "Unnamed field");
    Optional<Value> value = Optional.ofNullable(record.getValuesOrDefault(elementId, null));
    switch (f.getFieldTypeCase()) {
      case TEXT_FIELD:
        addTextField(elementId, label, value, f.getTextField(), f.getRequired());
        break;
      case MULTIPLE_CHOICE:
        addMultipleChoice(elementId, label, value, f.getMultipleChoice(), f.getRequired());
        break;
      default:
        Log.d(TAG, "Skipping unsupported field type: " + f.getFieldTypeCase());
    }
  }

  private void addTextField(
      String elementId, String label, Optional<Value> value, TextField f, boolean required) {
    // TODO: i18n.

    TextFieldView textInput =
        (TextFieldView) inflater.inflate(R.layout.text_field, contents, false);
    textInput.init(elementId, label, value, required);
    contents.addView(textInput);
  }

  private void addMultipleChoice(
      String elementId, String label, Optional<Value> value, MultipleChoice f, boolean required) {
    MultipleChoiceFieldView view =
        (MultipleChoiceFieldView) inflater.inflate(R.layout.multiple_choice_field, contents, false);
    view.init(elementId, label, value, f, required);
    contents.addView(view);
  }

  public void setMode(Editable.Mode mode) {
    if (this.mode != mode) {
      this.mode = mode;
      editableChildren().forEach(e -> e.setMode(mode));
      menuBtn.setVisibility(mode == EDIT ? INVISIBLE : VISIBLE);
    }
  }

  private void addValueUpdates(RecordUpdate.Builder recordUpdate) {
    editableChildren()
        .forEach(
            editable -> {
              ValueUpdate valueUpdate = editable.getUpdate();
              if (valueUpdate.getOperation() != NO_CHANGE) {
                recordUpdate.addValueUpdates(valueUpdate);
              }
            });
  }

  private Stream<Editable> editableChildren() {
    return children(contents).filter(Editable.class::isInstance).map(Editable.class::cast);
  }

  public RecordUpdate getUpdates() {
    RecordUpdate.Builder update = RecordUpdate.newBuilder();
    update.setRecord(record);
    addValueUpdates(update);
    if (update.getValueUpdatesCount() == 0) {
      update.setOperation(NO_CHANGE);
    } else if (record.getId().isEmpty()) {
      update.setOperation(CREATE);
    } else {
      update.setOperation(UPDATE);
    }
    return update.build();
  }

  public void setFocus() {
    editableChildren().findFirst().ifPresent(Editable::setFocus);
  }

  public void updateValidationMessages() {
    editableChildren().forEach(e -> e.updateValidationMessage());
  }

  public Mode getMode() {
    return mode;
  }

  public boolean isValid() {
    return editableChildren().allMatch(v -> v.isValid());
  }
}
