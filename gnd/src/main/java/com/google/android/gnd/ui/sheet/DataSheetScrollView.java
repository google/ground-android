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

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.model.FeatureUpdate;
import com.google.android.gnd.model.Form;
import com.google.android.gnd.ui.OnSheetSlideBehavior.SheetSlideMetrics;
import com.google.android.gnd.ui.sheet.input.Editable.Mode;
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.google.android.gnd.ui.OnSheetSlideBehavior.SheetSlideMetrics.scale;
import static com.google.android.gnd.ui.util.ViewUtil.children;
import static com.google.android.gnd.ui.util.ViewUtil.getScreenHeight;
import static com.google.android.gnd.ui.util.ViewUtil.getScreenWidth;

/** Container for data sheet components. */
// TODO: Use with RecyclerView?
public class DataSheetScrollView extends NestedScrollView {
  private static final float COLLAPSED_MAP_ASPECT_RATIO = 16.0f / 9.0f;

  @BindView(R.id.data_sheet_layout)
  LinearLayout layout;

  private Runnable onHideListener;
  private Runnable onShowListener;
  private RecordListPagerAdapter recordListPagerAdapter;
  private ViewPager recordListPager;
  private TabLayout tabLayout;
  private BottomSheetBehavior<DataSheetScrollView> behavior;

  @BindView(R.id.data_sheet_body)
  DataSheetBody body;

  @BindView(R.id.data_sheet_header)
  DataSheetHeader header;

  private Toolbar toolbar;
  private Mode mode;
  private boolean saved;

  public DataSheetScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    ButterKnife.bind(this);

    //    recordListPager = findViewById(R.id.record_list_pager);
    //    tabLayout = findViewById(R.id.record_list_tab_layout);
    //    tabLayout.setupWithViewPager(recordListPager);
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();

    // TODO: Sheet moves down when soft input method appears, sometimes too little maps shows.. Fix!
    behavior = BottomSheetBehavior.from(this);
    behavior.setBottomSheetCallback(new DataSheetCallback());
    behavior.setHideable(true);
    behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
  }

  public RecordListPagerAdapter getRecordListPagerAdapter() {
    return recordListPagerAdapter;
  }

  public DataSheetHeader getHeader() {
    return header;
  }

  public DataSheetBody getBody() {
    return body;
  }

  public void slideOpen() {
    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
  }

  public void hide() {
    behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
  }

  public void setOnHideListener(Runnable callback) {
    this.onHideListener = callback;
  }

  public void setOnShowListener(Runnable callback) {
    this.onShowListener = callback;
  }

  public void updateFormSelectorTabs(List<Form> forms) {
    // TODO: Finish implementing and move into Tabs class (this class should just be a container).
    RecordListPagerAdapter adapter =
        new RecordListPagerAdapter(
            ((MainActivity) getContext()).getSupportFragmentManager(), getContext(), forms);
    recordListPager.setAdapter(adapter);
    TabLayoutHelper tabLayoutHelper = new TabLayoutHelper(tabLayout, recordListPager);
    tabLayoutHelper.setAutoAdjustTabModeEnabled(true);
  }

  public FeatureUpdate getUpdates() {
    FeatureUpdate.Builder updates = header.getFeatureUpdateBuilder();
    updates.addAllRecordUpdates(body.getUpdates());
    return updates.build();
  }

  public void setMode(Mode mode) {
    this.mode = mode;
//    int initialDataPanelHeight = (int) (screenHeight * COLLAPSED_MAP_ASPECT_RATIO);
    double width = getScreenWidth((Activity) getContext());
    double screenHeight = getScreenHeight((Activity) getContext());
    double mapHeight = width / COLLAPSED_MAP_ASPECT_RATIO;
    double peekHeight = screenHeight - mapHeight;
    // TODO: Take window insets into account; COLLAPSED_MAP_ASPECT_RATIO will be wrong on older devices w/o
    // translucent system windows.
    behavior.setPeekHeight((int) peekHeight);
    updateUiForSlide();
  }

  public Mode getMode() {
    return mode;
  }

  public boolean isValid() {
    return body.isValid();
  }

  public void updateValidationMessages() {
    body.updateValidationMessages();
  }

  public boolean isModified() {
    return !body.isSaved() && !body.getUpdates().isEmpty();
  }

  public void markAsSaved() {
    body.markAsSaved();
  }

  private class DataSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
    private boolean shown;

    @Override
    public void onStateChanged(@NonNull View bottomSheet, int newState) {
      switch (newState) {
        case BottomSheetBehavior.STATE_COLLAPSED:
          if (!shown && onShowListener != null) {
            shown = true;
            onShowListener.run();
          }
          break;
        case BottomSheetBehavior.STATE_HIDDEN:
          if (shown && onHideListener != null) {
            shown = false;
            onHideListener.run();
          }
          break;
      }
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
      updateUiForSlide();
    }
  }

  private void updateUiForSlide() {
    SheetSlideMetrics metrics = new SheetSlideMetrics(this);
    toolbar = ((MainActivity) getContext()).getToolbar();
    FrameLayout toolbarWrapper = (FrameLayout) toolbar.getParent();
    toolbarWrapper.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
    toolbarWrapper.setTranslationY(
        metrics.scale(metrics.getVisibleRatio(), 0.3f, 0.5f, -toolbarWrapper.getHeight(), 0));
    if (mode == Mode.VIEW) {
      metrics.showWithSheet(toolbarWrapper.getBackground(), 0.9f, 1);
      float alpha = scale(getTop(), 0, toolbar.getHeight(), 1f, 0f);
      // Fade in toolbar text labels with sheet expansion.
      children(toolbar).filter(TextView.class::isInstance).forEach(v -> v.setAlpha(alpha));
      // Fade out header text labels with sheet expansion.
    } else {
      toolbarWrapper.setAlpha(1.0f);
      children(toolbar).filter(TextView.class::isInstance).forEach(v -> v.setAlpha(1.0f));
    }
  }
}
