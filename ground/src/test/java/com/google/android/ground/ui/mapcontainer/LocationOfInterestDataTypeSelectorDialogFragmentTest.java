/*
 * Copyright 2022 Google LLC
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

package com.google.android.ground.ui.mapcontainer;

import static android.os.Looper.getMainLooper;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.view.View;
import android.widget.ListView;
import com.google.android.ground.BaseHiltTest;
import com.google.android.ground.MainActivity;
import com.google.android.ground.R;
import com.google.android.ground.ui.home.mapcontainer.LocationOfInterestDataTypeSelectorDialogFragment;
import com.google.android.ground.ui.surveyselector.SurveySelectorDialogFragment;
import dagger.hilt.android.testing.HiltAndroidTest;
import java8.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class LocationOfInterestDataTypeSelectorDialogFragmentTest extends BaseHiltTest {

  private LocationOfInterestDataTypeSelectorDialogFragment
      locationOfInterestDataTypeSelectorDialogFragment;

  private Consumer<Integer> onSelectLoiDataType;
  private int selectedPosition = -1;

  @Before
  public void setUp() {
    super.setUp();

    onSelectLoiDataType = integer -> selectedPosition = integer;

    setUpFragment();
  }

  private void setUpFragment() {
    ActivityController<MainActivity> activityController =
        Robolectric.buildActivity(MainActivity.class);
    MainActivity activity = activityController.setup().get();

    locationOfInterestDataTypeSelectorDialogFragment =
        new LocationOfInterestDataTypeSelectorDialogFragment(onSelectLoiDataType);

    locationOfInterestDataTypeSelectorDialogFragment.showNow(
        activity.getSupportFragmentManager(), SurveySelectorDialogFragment.class.getSimpleName());
    shadowOf(getMainLooper()).idle();
  }

  @Test
  public void show_dialogIsShown() {
    View listView = locationOfInterestDataTypeSelectorDialogFragment.getDialog().getCurrentFocus();

    assertThat(listView).isNotNull();
    assertThat(listView.getVisibility()).isEqualTo(View.VISIBLE);
    assertThat(listView.findViewById(R.id.survey_name).getVisibility()).isEqualTo(View.VISIBLE);
  }

  @Test
  public void show_dataTypeSelected_correctDataTypeIsPassed() {
    ListView listView =
        (ListView) locationOfInterestDataTypeSelectorDialogFragment.getDialog().getCurrentFocus();

    int positionToSelect = 1;
    shadowOf(listView).performItemClick(positionToSelect);
    shadowOf(getMainLooper()).idle();

    // Verify Dialog is dismissed
    assertThat(locationOfInterestDataTypeSelectorDialogFragment.getDialog()).isNull();

    assertThat(selectedPosition).isEqualTo(1);
  }
}
