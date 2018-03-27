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

package com.google.gnd.view.sheet;

import android.content.Context;
import android.support.annotation.Dimension;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gnd.R;
import com.google.gnd.model.Feature;
import com.google.gnd.model.FeatureType;
import com.google.gnd.model.FeatureUpdate;
import com.google.protobuf.Timestamp;

import static com.google.gnd.model.FeatureUpdate.Operation.CREATE;
import static com.google.gnd.model.FeatureUpdate.Operation.NO_CHANGE;
import static com.google.gnd.model.FeatureUpdate.Operation.UPDATE;

public class DataSheetHeader extends LinearLayout {
  private TextView titleView;
  private TextView subtitleView;
  private Feature originalValue;

  public DataSheetHeader(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    titleView = findViewById(R.id.data_sheet_title);
    subtitleView = findViewById(R.id.data_sheet_subtitle);
  }

  // TODO: Nest FeatureType into Feature?
  public void attach(Feature feature, FeatureType featureType) {
    // TODO: i18n.
    originalValue = feature;
  }

  public Feature getCurrentValue() {
    return originalValue;
  }

  public FeatureUpdate.Builder getFeatureUpdateBuilder() {
    FeatureUpdate.Builder update = FeatureUpdate.newBuilder();
    update.setClientTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000));
    Feature feature = getCurrentValue();
    update.setFeature(feature);
    if (feature.getId().isEmpty()) {
      update.setOperation(CREATE);
    } else if (!feature.equals(originalValue)) {
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
