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

import static com.google.android.gnd.model.PlaceUpdate.Operation.CREATE;
import static com.google.android.gnd.model.PlaceUpdate.Operation.NO_CHANGE;
import static com.google.android.gnd.model.PlaceUpdate.Operation.UPDATE;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gnd.R;
import com.google.android.gnd.model.Place;
import com.google.android.gnd.model.PlaceType;
import com.google.android.gnd.model.PlaceUpdate;
import com.google.protobuf.Timestamp;

public class PlaceSheetHeader extends LinearLayout {
  private TextView titleView;
  private TextView subtitleView;
  private Place originalValue;

  public PlaceSheetHeader(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    titleView = findViewById(R.id.data_sheet_title);
    subtitleView = findViewById(R.id.data_sheet_subtitle);
  }

  // TODO: Nest PlaceType into Place?
  public void attach(Place place, PlaceType placeType) {
    // TODO: i18n.
    originalValue = place;
  }

  public Place getCurrentValue() {
    return originalValue;
  }

  public PlaceUpdate.Builder getPlaceUpdateBuilder() {
    PlaceUpdate.Builder update = PlaceUpdate.newBuilder();
    update.setClientTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000));
    Place place = getCurrentValue();
    update.setPlace(place);
    if (place.getId().isEmpty()) {
      update.setOperation(CREATE);
    } else if (!place.equals(originalValue)) {
      update.setOperation(UPDATE);
    } else {
      update.setOperation(NO_CHANGE);
    }
    return update;
  }

  public void setTitle(String title) {
    titleView.setText(title);
  }

  public void setSubtitle(String subtitle) {
    subtitleView.setText(subtitle);
  }

  public void setHeaderTextAlpha(float alpha) {
    titleView.setAlpha(alpha);
    subtitleView.setAlpha(alpha);
  }
}
