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

package com.google.android.gnd.ui.common;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.gnd.R;

// TODO: Improve logic for toolbar heading and possibly extra line below.
public class TwoLineToolbar extends Toolbar {

  @BindView(R.id.toolbar_title_text)
  TextView titleText;

  @BindView(R.id.toolbar_subtitle_text)
  TextView subtitleText;

  public TwoLineToolbar(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    inflate(getContext(), R.layout.two_line_toolbar, this);
    ButterKnife.bind(this);
  }

  public void setTitle(String title) {
    titleText.setText(title);
  }

  public void setSubtitle(String subtitle) {
    subtitleText.setText(subtitle);
    subtitleText.setVisibility(subtitle.isEmpty() ? View.GONE : View.VISIBLE);
  }
}
