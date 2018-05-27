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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.View;
import butterknife.ButterKnife;

// TODO: Use with RecyclerView?
public class PlaceSheetScrollView extends NestedScrollView {
  private static final float COLLAPSED_MAP_ASPECT_RATIO = 16.0f / 9.0f;

  //  @BindView(R.id.data_sheet_layout)
  //  LinearLayout layout;

  private Runnable onHideListener;
  private Runnable onShowListener;
  private RecordListPagerAdapter recordListPagerAdapter;
  private ViewPager recordListPager;
  private TabLayout tabLayout;
  private BottomSheetBehavior<PlaceSheetScrollView> behavior;

  //  @BindView(R.id.data_sheet_body)
  //  PlaceSheetBody body;
  //
  //  @BindView(R.id.data_sheet_header)
  //  PlaceSheetHeader header;

  //  private Toolbar toolbar;
  //  private Mode mode;
  //  private boolean saved;

  public PlaceSheetScrollView(Context context, AttributeSet attrs) {
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

  //  public RecordListPagerAdapter getRecordListPagerAdapter() {
  //    return recordListPagerAdapter;
  //  }

  //  public PlaceSheetHeader getHeader() {
  //    return header;
  //  }

  //  public PlaceSheetBody getBody() {
  //    return body;
  //  }

  private void show() {
    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
  }

  private void hide() {
    behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
  }

  //  public void setOnHideListener(Runnable callback) {
  //    this.onHideListener = callback;
  //  }

  //  public void setOnShowListener(Runnable callback) {
  //    this.onShowListener = callback;
  //  }

  //  public void updateFormSelectorTabs(List<Form> forms) {
  //     TODO: Finish implementing and move into Tabs class (this class should just be a container).
  //    RecordListPagerAdapter adapter =
  //        new RecordListPagerAdapter(
  //            ((MainActivity) getContext()).getSupportFragmentManager(), getContext(), forms);
  //    recordListPager.setAdapter(adapter);
  //    TabLayoutHelper tabLayoutHelper = new TabLayoutHelper(tabLayout, recordListPager);
  //    tabLayoutHelper.setAutoAdjustTabModeEnabled(true);
  //  }

  //  public PlaceUpdate getUpdates() {
  //    PlaceUpdate.Builder updates = header.getPlaceUpdateBuilder();
  //    updates.addAllRecordUpdates(body.getUpdates());
  //    return updates.build();
  //  }
  //
  //  public void setMode(Mode mode) {
  //    this.mode = mode;
  //    //    int initialDataPanelHeight = (int) (screenHeight * COLLAPSED_MAP_ASPECT_RATIO);
  //    double width = getScreenWidth((Activity) getContext());
  //    double screenHeight = getScreenHeight((Activity) getContext());
  //    double mapHeight = width / COLLAPSED_MAP_ASPECT_RATIO;
  //    double peekHeight = screenHeight - mapHeight;
  //    // TODO: Take window insets into account; COLLAPSED_MAP_ASPECT_RATIO will be wrong on older
  //    // devices w/o
  //    // translucent system windows.
  //    behavior.setPeekHeight((int) peekHeight);
  //    updateUiForSlide();
  //  }

  //  public Mode getMode() {
  //    return mode;
  //  }

  //  public boolean isValid() {
  //    return body.isValid();
  //  }

  //  public void updateValidationMessages() {
  //    body.updateValidationMessages();
  //  }

  //  public boolean isModified() {
  //    return !body.isSaved() && !body.getUpdates().isEmpty();
  //  }

  //  public void markAsSaved() {
  //    body.markAsSaved();
  //  }

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
    //    SheetSlideMetrics metrics = new SheetSlideMetrics(this);
    //    toolbar = ((MainActivity) getContext()).getToolbar();
    //    FrameLayout toolbarWrapper = (FrameLayout) toolbar.getParent();
    //    toolbarWrapper.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
    //    toolbarWrapper.setTranslationY(
    //        scale(metrics.getVisibleRatio(), 0.3f, 0.5f, -toolbarWrapper.getHeight(), 0));
    //    if (mode == Mode.VIEW) {
    //      metrics.showWithSheet(toolbarWrapper.getBackground(), 0.9f, 1);
    //      float alpha = scale(getTop(), 0, toolbar.getHeight(), 1f, 0f);
    //      // Fade in toolbar text labels with sheet expansion.
    //      children(toolbar).filter(TextView.class::isInstance).forEach(v -> v.setAlpha(alpha));
    //      // Fade out header text labels with sheet expansion.
    //    } else {
    //      toolbarWrapper.setAlpha(1.0f);
    //      children(toolbar).filter(TextView.class::isInstance).forEach(v -> v.setAlpha(1.0f));
    //    }
  }
}
