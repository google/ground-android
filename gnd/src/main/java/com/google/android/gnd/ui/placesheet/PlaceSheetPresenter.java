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

package com.google.android.gnd.ui.placesheet;

import static com.google.android.gnd.service.firestore.FirestoreDataService.toDate;
import static com.google.android.gnd.ui.util.ViewUtil.children;

import static java8.util.stream.StreamSupport.stream;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.model.Form;
import com.google.android.gnd.model.GndDataRepository;
import com.google.android.gnd.model.Place;
import com.google.android.gnd.model.PlaceType;
import com.google.android.gnd.model.Record;
import com.google.android.gnd.ui.MainPresenter;
import com.google.android.gnd.ui.UserOperationFailed;
import com.google.android.gnd.ui.map.MapAdapter;
import com.google.android.gnd.ui.map.MapAdapter.Map;
import com.google.android.gnd.ui.placesheet.input.Editable;
import com.google.android.gnd.ui.placesheet.input.Editable.Mode;

import java8.util.Optional;

public class PlaceSheetPresenter {

  private static final String TAG = PlaceSheetPresenter.class.getSimpleName();

  private final MainPresenter mainPresenter;
  private final MainActivity mainActivity;
  private final GndDataRepository model;
  private PlaceSheetScrollView dataSheetView;
  private Toolbar toolbar;
  private MapAdapter mapAdapter;
  private FloatingActionButton addRecordBtn;
  private FloatingActionButton addPlaceBtn;

  public PlaceSheetPresenter(
      MainPresenter mainPresenter, MainActivity mainActivity, GndDataRepository model) {
    this.mainPresenter = mainPresenter;
    this.mainActivity = mainActivity;
    this.model = model;
  }

  public void onCreate(Bundle savedInstanceState) {
    dataSheetView = mainActivity.getDataSheetView();
    addRecordBtn = mainActivity.getAddRecordButton();
    // TODO: Move access to these through mainActivity?
    addPlaceBtn = mainActivity.getAddPlaceButton();
    toolbar = mainActivity.getToolbar();
  }

  public void onReady() {
    dataSheetView.setOnHideListener(this::onHideSheet);
    dataSheetView.setOnShowListener(this::onShowSheet);
    addRecordBtn.setVisibility(View.GONE);
    addRecordBtn.setOnClickListener((v) -> this.onAddRecordClick());
    toolbar.setNavigationOnClickListener((v) -> this.onBackClick());
  }

  private void onBackClick() {
    dataSheetView.hide();
  }

  public void onHideSheet() {
    if (dataSheetView.getMode() == Mode.EDIT && dataSheetView.isModified()) {
      AlertDialog alertDialog = new AlertDialog.Builder(mainActivity).create();
      alertDialog.setMessage(mainActivity.getResources().getString(R.string.unsaved_changes));
      alertDialog.setButton(
          AlertDialog.BUTTON_POSITIVE,
          mainActivity.getResources().getString(R.string.save_unsaved_changes),
          (dialog, which) -> {
            if (validateForm()) {
              saveFormData();
              reenableMap();
            } else {
              dataSheetView.updateValidationMessages();
              dataSheetView.slideOpen();
            }
          });
      alertDialog.setButton(
          AlertDialog.BUTTON_NEGATIVE,
          mainActivity.getResources().getString(R.string.continue_editing),
          (dialog, which) -> {
            dataSheetView.updateValidationMessages();
            dataSheetView.slideOpen();
          });
      alertDialog.setButton(
          AlertDialog.BUTTON_NEUTRAL,
          mainActivity.getResources().getString(R.string.close_without_saving),
          (dialog, which) -> reenableMap());
      alertDialog.show();
      return;
    }
    reenableMap();
  }

  private void reenableMap() {
    addRecordBtn.setVisibility(View.GONE);
    // TODO: Hide temporary place marker.
    mainActivity.hideSoftInput();
    addRecordBtn.setEnabled(false);
    addPlaceBtn.setEnabled(true);
    mapAdapter.map().subscribe(Map::enable);
  }

  public void onShowSheet() {
    mapAdapter.map().subscribe(MapAdapter.Map::disable);
    //    mapAdapter.pauseLocationUpdates()
    addPlaceBtn.setEnabled(false);
    addRecordBtn.setEnabled(true);
  }

  public void showPlaceDetails(Place place) throws UserOperationFailed {
    PlaceType placeType = getPlaceType(place);
    addRecordBtn.setVisibility(View.VISIBLE);
    hideToolbarSaveButton();
    updateToolbarHeadings(place, placeType);

    dataSheetView.setMode(Editable.Mode.VIEW);
    dataSheetView.getHeader().attach(place, placeType);
    dataSheetView.getHeader().setTitle(getTitle(place, placeType));
    dataSheetView.getHeader().setSubtitle(getSubtitle(place, placeType));
    //    dataSheetView.refreshFormSelectorTabs(placeType.getFormsList());
    dataSheetView.getBody().clear();
    // TODO: Loading spinner.
    // TODO: Implement pagination? i.e., Only load n at a time?
    model
        .getRecordData(place.getId())
        .thenAccept(
            records -> {
              stream(records)
                  .sorted(
                      (o1, o2) ->
                          toDate(o2.getServerTimestamps().getModified())
                              .compareTo(toDate(o1.getServerTimestamps().getModified())))
                  .forEach(
                      record -> {
                        RecordView formView =
                            new RecordView(dataSheetView.getContext(), this::onEditRecordClick);
                        formView.populate(placeType.getForms(0), record, Editable.Mode.VIEW);
                        dataSheetView.getBody().addView(formView);
                      });
            });
    // TODO: Show spinner while loading.
    // TODO: Support multiple forms.
    dataSheetView.slideOpen();
  }

  @NonNull
  private PlaceType getPlaceType(Place place) throws UserOperationFailed {
    Optional<PlaceType> placeType = model.getPlaceType(place.getPlaceTypeId());
    if (!placeType.isPresent()) {
      throw new UserOperationFailed("Place type unknown for place " + place.getId());
    }
    return placeType.get();
  }

  private void updateToolbarHeadings(Place place, PlaceType placeType) {
    // TODO: i18n.
    toolbar.setTitle(getTitle(place, placeType));
    toolbar.setSubtitle(getSubtitle(place, placeType));
  }

  public String getTitle(Place place, PlaceType placeType) {
    // TODO: i18n.
    String placeTypeLabel = placeType.getItemLabelOrDefault("pt", "");
    String caption = place.getCaption();
    return caption.isEmpty() ? placeTypeLabel : caption;
  }

  public String getSubtitle(Place place, PlaceType placeType) {
    // TODO: i18n.
    String placeTypeLabel = placeType.getItemLabelOrDefault("pt", "");
    String caption = place.getCaption();
    return caption.isEmpty() ? "" : placeTypeLabel + " " + place.getCustomId();
  }

  private void onAddRecordClick() {
    try {
      Place place = dataSheetView.getHeader().getCurrentValue();
      PlaceType placeType = getPlaceType(place);
      RecordView recordView = new RecordView(dataSheetView.getContext(), this::onEditRecordClick);
      // TODO: Hide add record button if there are no forms available.
      if (placeType.getFormsCount() > 0) {
        // TODO: Support multiple form types.
        Form form = placeType.getForms(0);
        Record record =
            Record.newBuilder().setPlaceTypeId(placeType.getId()).setFormId(form.getId()).build();
        recordView.populate(form, record, Editable.Mode.EDIT);
        dataSheetView.getBody().addView(recordView);
        onEditRecordClick(recordView);
      }
    } catch (UserOperationFailed e) {
      Log.e(TAG, e.getMessage());
    }
  }

  private void onEditRecordClick(RecordView recordView) {
    hideOtherRecordViews(recordView);
    toolbar.setTitle(R.string.add_place_data_toolbar_title);
    toolbar.setSubtitle("");
    dataSheetView.setMode(Mode.EDIT);
    recordView.setMode(Mode.EDIT);
    showToolbarSaveButton();
    addRecordBtn.setVisibility(View.GONE);
  }

  private void hideOtherRecordViews(RecordView recordView) {
    // HACK: This will go away add/edit place has its own Fragment.
    children(dataSheetView.getBody())
        .filter(v -> v != recordView)
        .forEach(v -> v.setVisibility(View.GONE));
  }

  public void onSelectPlaceTypeForAdd(PlaceType placeType) {
    mapAdapter
        .map()
        .subscribe(
            map -> {
              Place place =
                  Place.newBuilder()
                      .setPlaceTypeId(placeType.getId())
                      .setPoint(map.getCenter())
                      .build();
              showToolbarSaveButton();
              // TODO: i18n.
              toolbar.setTitle(R.string.add_place_toolbar_title);
              toolbar.setSubtitle("");
              addRecordBtn.setVisibility(View.INVISIBLE);
              // TODO: Encapsulate placeType, place, etc. as SheetState or in ApplicationState?
              dataSheetView.setMode(Editable.Mode.EDIT);
              dataSheetView.getHeader().attach(place, placeType);
              dataSheetView.getHeader().setTitle(getTitle(place, placeType));
              dataSheetView.getHeader().setSubtitle("");
              dataSheetView.getBody().clear();
              // TODO: Show temporary highlighted place marker.
              RecordView formView =
                  new RecordView(dataSheetView.getContext(), this::onEditRecordClick);
              if (placeType.getFormsCount() > 0) {
                // TODO: Support multiple form types.
                Form form = placeType.getForms(0);
                Record record =
                    Record.newBuilder()
                        .setPlaceTypeId(placeType.getId())
                        .setFormId(form.getId())
                        .build();
                formView.populate(form, record, Editable.Mode.EDIT);
                dataSheetView.getBody().addView(formView);
              }
              // TODO: Do something smart when there are no forms associated with the place.
              dataSheetView.slideOpen();
            });
  }

  private void hideToolbarSaveButton() {
    mainActivity.getToolbarSaveButton().setVisible(false);
  }

  private void showToolbarSaveButton() {
    mainActivity.getToolbarSaveButton().setVisible(true);
  }

  public boolean onSaveClick() {
    if (validateForm()) {
      saveFormData();
      return true;
    }
    dataSheetView.updateValidationMessages();
    return false;
  }

  private boolean validateForm() {
    if (dataSheetView.isValid()) {
      return true;
    }
    AlertDialog alertDialog = new AlertDialog.Builder(mainActivity).create();
    alertDialog.setMessage(mainActivity.getResources().getString(R.string.invalid_data));
    alertDialog.setButton(
        AlertDialog.BUTTON_POSITIVE,
        mainActivity.getResources().getString(R.string.invalid_data_ok),
        (dialog, which) -> dialog.dismiss());
    alertDialog.show();
    return false;
  }

  private boolean saveFormData() {
    if (!dataSheetView.isValid()) {
      return true;
    }
    model.update(dataSheetView.getUpdates());
    dataSheetView.markAsSaved();
    dataSheetView.hide();
    Snackbar.make(dataSheetView, R.string.save_queued, Snackbar.LENGTH_SHORT).show();
    return false;
  }
}
