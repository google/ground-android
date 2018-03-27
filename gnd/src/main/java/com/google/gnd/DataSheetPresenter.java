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

package com.google.gnd;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.gnd.model.DataModel;
import com.google.gnd.model.Feature;
import com.google.gnd.model.FeatureType;
import com.google.gnd.model.Form;
import com.google.gnd.model.PlaceIcon;
import com.google.gnd.model.Record;
import com.google.gnd.view.map.GoogleMapsView;
import com.google.gnd.view.map.MapMarker;
import com.google.gnd.view.sheet.DataSheetScrollView;
import com.google.gnd.view.sheet.RecordView;
import com.google.gnd.view.sheet.input.Editable;
import com.google.gnd.view.sheet.input.Editable.Mode;

import java8.util.Optional;

import static com.google.gnd.service.firestore.FirestoreDataService.toDate;
import static com.google.gnd.view.util.ViewUtil.children;
import static java8.util.stream.StreamSupport.stream;

public class DataSheetPresenter {
  private static final String TEMP_MARKER_ID = ":tempMarker";
  private static final float UNSELECTED_MARKER_ALPHA = 0.4f;
  private static final String TAG = DataSheetPresenter.class.getSimpleName();

  private final MainPresenter mainPresenter;
  private final MainActivity mainActivity;
  private final DataModel model;
  private DataSheetScrollView dataSheetView;
  private Toolbar toolbar;
  private GoogleMapsView mapView;
  private FloatingActionButton addRecordBtn;
  private FloatingActionButton addFeatureBtn;

  public DataSheetPresenter(
      MainPresenter mainPresenter, MainActivity mainActivity, DataModel model) {
    this.mainPresenter = mainPresenter;
    this.mainActivity = mainActivity;
    this.model = model;
  }

  void onCreate(Bundle savedInstanceState) {
    dataSheetView = mainActivity.getDataSheetView();
    addRecordBtn = mainActivity.getAddRecordButton();
    // TODO: Move access to these through mainActivity?
    addFeatureBtn = mainActivity.getAddFeatureButton();
    toolbar = mainActivity.getToolbar();
    mapView = mainActivity.getMapView();
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
    mapView.removeMarker(TEMP_MARKER_ID);
    mapView.setOtherMarkersAlpha(1.0f, null);
    mainActivity.hideSoftInput();
    addRecordBtn.setEnabled(false);
    addFeatureBtn.setEnabled(true);
    mapView.enable();
  }

  public void onShowSheet() {
    mapView.disable();
    //    mapView.pauseLocationUpdates()
    addFeatureBtn.setEnabled(false);
    addRecordBtn.setEnabled(true);
  }

  public void showFeatureDetails(Feature feature) throws UserOperationFailed {
    FeatureType featureType = getFeatureType(feature);
    addRecordBtn.setVisibility(View.VISIBLE);
    hideToolbarSaveButton();
    updateToolbarHeadings(feature, featureType);

    dataSheetView.setMode(Editable.Mode.VIEW);
    dataSheetView.getHeader().attach(feature, featureType);
    dataSheetView.getHeader().setTitle(getTitle(feature, featureType));
    dataSheetView.getHeader().setSubtitle(getSubtitle(feature, featureType));
    //    dataSheetView.refreshFormSelectorTabs(featureType.getFormsList());
    dataSheetView.getBody().clear();
    mapView.setOtherMarkersAlpha(UNSELECTED_MARKER_ALPHA, feature.getId());
    // TODO: Loading spinner.
    // TODO: Implement pagination? i.e., Only load n at a time?
    model
        .getRecordData(feature.getId())
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
                        formView.populate(featureType.getForms(0), record, Editable.Mode.VIEW);
                        dataSheetView.getBody().addView(formView);
                      });
            });
    // TODO: Show spinner while loading.
    // TODO: Support multiple forms.
    dataSheetView.slideOpen();
  }

  @NonNull
  private FeatureType getFeatureType(Feature feature) throws UserOperationFailed {
    Optional<FeatureType> featureType = model.getFeatureType(feature.getFeatureTypeId());
    if (!featureType.isPresent()) {
      throw new UserOperationFailed("Feature type unknown for feature " + feature.getId());
    }
    return featureType.get();
  }

  private void updateToolbarHeadings(Feature feature, FeatureType featureType) {
    // TODO: i18n.
    toolbar.setTitle(getTitle(feature, featureType));
    toolbar.setSubtitle(getSubtitle(feature, featureType));
  }

  public String getTitle(Feature feature, FeatureType featureType) {
    // TODO: i18n.
    String placeTypeLabel = featureType.getItemLabelOrDefault("pt", "");
    String caption = feature.getCaption();
    return caption.isEmpty() ? placeTypeLabel : caption;
  }

  public String getSubtitle(Feature feature, FeatureType featureType) {
    // TODO: i18n.
    String placeTypeLabel = featureType.getItemLabelOrDefault("pt", "");
    String caption = feature.getCaption();
    return caption.isEmpty() ? "" : placeTypeLabel + " " + feature.getCustomId();
  }

  private void onAddRecordClick() {
    try {
      Feature feature = dataSheetView.getHeader().getCurrentValue();
      FeatureType featureType = getFeatureType(feature);
      RecordView recordView = new RecordView(dataSheetView.getContext(), this::onEditRecordClick);
      // TODO: Hide add record button if there are no forms available.
      if (featureType.getFormsCount() > 0) {
        // TODO: Support multiple form types.
        Form form = featureType.getForms(0);
        Record record =
            Record.newBuilder()
                .setFeatureTypeId(featureType.getId())
                .setFormId(form.getId())
                .build();
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

  public void onSelectFeatureTypeForAdd(FeatureType featureType) {
    Feature feature =
        Feature.newBuilder()
            .setFeatureTypeId(featureType.getId())
            .setPoint(mainActivity.getMapView().getCenter())
            .build();
    showToolbarSaveButton();
    // TODO: i18n.
    toolbar.setTitle(R.string.add_place_toolbar_title);
    toolbar.setSubtitle("");
    addRecordBtn.setVisibility(View.INVISIBLE);
    // TODO: Encapsulate featureType, feature, etc. as SheetState or in ApplicationState?
    dataSheetView.setMode(Editable.Mode.EDIT);
    dataSheetView.getHeader().attach(feature, featureType);
    dataSheetView.getHeader().setTitle(getTitle(feature, featureType));
    dataSheetView.getHeader().setSubtitle("");
    dataSheetView.getBody().clear();
    // TODO: Move alpha to resource.
    mapView.setOtherMarkersAlpha(UNSELECTED_MARKER_ALPHA, null);
    MapMarker<Feature> mapMarker =
        new MapMarker(
            TEMP_MARKER_ID,
            feature.getPoint(),
            new PlaceIcon(dataSheetView.getContext(), featureType.getIconId(), 0),
            feature);
    mapView.addOrUpdateMarker(mapMarker, true, true);
    RecordView formView = new RecordView(dataSheetView.getContext(), this::onEditRecordClick);
    if (featureType.getFormsCount() > 0) {
      // TODO: Support multiple form types.
      Form form = featureType.getForms(0);
      Record record =
          Record.newBuilder().setFeatureTypeId(featureType.getId()).setFormId(form.getId()).build();
      formView.populate(form, record, Editable.Mode.EDIT);
      dataSheetView.getBody().addView(formView);
    }
    // TODO: Do something smart when there are no forms associated with the feature.
    dataSheetView.slideOpen();
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
