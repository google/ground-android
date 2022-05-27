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

package com.google.android.gnd.ui.mapcontainer;

import static android.os.Looper.getMainLooper;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.view.View;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.home.mapcontainer.PolygonDrawingInfoDialogFragment;
import com.google.android.gnd.ui.surveyselector.SurveySelectorDialogFragment;
import dagger.hilt.android.testing.HiltAndroidTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class PolygonDrawingInfoDialogFragmentTest extends BaseHiltTest {

  private PolygonDrawingInfoDialogFragment polygonDrawingInfoDialogFragment;

  private Runnable onClickRunnable;
  private boolean clicked;

  @Before
  public void setUp() {
    super.setUp();

    onClickRunnable = () -> clicked = true;

    setUpFragment();
  }

  @After
  public void tearDown() {
    clicked = false;
  }

  private void setUpFragment() {
    ActivityController<MainActivity> activityController =
        Robolectric.buildActivity(MainActivity.class);
    MainActivity activity = activityController.setup().get();

    polygonDrawingInfoDialogFragment = new PolygonDrawingInfoDialogFragment(onClickRunnable);

    polygonDrawingInfoDialogFragment.showNow(
        activity.getSupportFragmentManager(), SurveySelectorDialogFragment.class.getSimpleName());
    shadowOf(getMainLooper()).idle();
  }

  @Test
  public void show_dialogIsShown() {
    View dialogView = polygonDrawingInfoDialogFragment.getDialog().getCurrentFocus();

    assertThat(dialogView).isNotNull();
    assertThat(dialogView.getVisibility()).isEqualTo(View.VISIBLE);
    assertThat(
            ((ConstraintLayout) dialogView.getParent())
                .findViewById(R.id.polygon_info_image)
                .getVisibility())
        .isEqualTo(View.VISIBLE);
  }

  @Test
  public void startClicked_clickIsRegisteredAndDialogIsDismissed() {
    ConstraintLayout dialogView =
        (ConstraintLayout)
            polygonDrawingInfoDialogFragment.getDialog().getCurrentFocus().getParent();

    shadowOf((View) dialogView.findViewById(R.id.get_started_button)).checkedPerformClick();
    shadowOf(getMainLooper()).idle();

    // Verify Dialog is dismissed
    assertThat(polygonDrawingInfoDialogFragment.getDialog()).isNull();

    assertThat(clicked).isTrue();
  }

  @Test
  public void cancelClicked_dialogIsDismissed() {
    ConstraintLayout dialogView =
        (ConstraintLayout)
            polygonDrawingInfoDialogFragment.getDialog().getCurrentFocus().getParent();

    shadowOf((View) dialogView.findViewById(R.id.cancel_text_view)).checkedPerformClick();
    shadowOf(getMainLooper()).idle();

    // Verify Dialog is dismissed
    assertThat(polygonDrawingInfoDialogFragment.getDialog()).isNull();
  }
}
