/*
 * Copyright 2021 Google LLC
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

package com.google.android.gnd.ui;

import static com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel.ZOOM_LEVEL_THRESHOLD;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.R;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.testing.HiltAndroidTest;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class MarkerIconFactoryTest extends BaseHiltTest {

  @Inject @ApplicationContext Context context;
  @Inject MarkerIconFactory markerIconFactory;

  private int markerUnscaledWidth;
  private int markerUnscaledHeight;

  @Before
  public void setUp() {
    super.setUp();
    Drawable outline = AppCompatResources.getDrawable(context, R.drawable.ic_marker_outline);
    markerUnscaledWidth = outline.getIntrinsicWidth();
    markerUnscaledHeight = outline.getIntrinsicHeight();
  }

  @Test
  public void getMarkerBitmap_zoomedOut_scaleIsSetCorrectly() {
    Bitmap bitmap = markerIconFactory.getMarkerBitmap(Color.BLUE, ZOOM_LEVEL_THRESHOLD - 0.1f);

    float scale =
        ResourcesCompat.getFloat(context.getResources(), R.dimen.marker_bitmap_default_scale);
    verifyBitmapScale(bitmap, scale);
  }

  @Test
  public void getMarkerBitmap_zoomedIn_scaleIsSetCorrectly() {
    Bitmap bitmap = markerIconFactory.getMarkerBitmap(Color.BLUE, ZOOM_LEVEL_THRESHOLD);

    float scale =
        ResourcesCompat.getFloat(context.getResources(), R.dimen.marker_bitmap_zoomed_scale);
    verifyBitmapScale(bitmap, scale);
  }

  private void verifyBitmapScale(Bitmap bitmap, float scale) {
    int expectedWidth = (int) (markerUnscaledWidth * scale);
    int expectedHeight = (int) (markerUnscaledHeight * scale);

    assertThat(bitmap.getWidth()).isEqualTo(expectedWidth);
    assertThat(bitmap.getHeight()).isEqualTo(expectedHeight);
  }
}
