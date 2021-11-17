/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

import com.google.android.gnd.system.auth.FakeAuthenticationManager;
import dagger.hilt.android.testing.HiltAndroidTest;
import javax.inject.Inject;
import org.junit.Ignore;
import org.junit.Test;

@HiltAndroidTest
public class AddFeatureTest extends BaseMainActivityTest {

  @Inject FakeAuthenticationManager fakeAuthenticationManager;

  @Override
  public void setUp() {
    super.setUp();
    fakeAuthenticationManager.setUser(FakeData.USER);
  }

  // Given: a logged in user - with an active project with no map markers.
  // When: they tap on the centre of the map.
  // Then: nothing happens - the feature fragment is not displayed.
  @Test
  public void tappingCrosshairOnEmptyMapDoesNothing() {
    dataBindingIdlingResource.monitorActivity(scenarioRule.getScenario());

    // Tap on the checkbox
    onView(withId(R.id.agreeCheckBox)).perform(click());

    // Tap on Submit on Terms Fragment
    onView(withId(R.id.agreeButton)).perform(click());

    // Tap on the cross-hair at the centre of the map.
    onView(withId(R.id.map_crosshairs_img)).perform(click());

    // Verify that the title is not displayed.
    onView(withId(R.id.feature_title)).check(matches(not(isCompletelyDisplayed())));
  }

  // Given: A logged in user with an active project
  // When: They tap the "Add feature" FAB and choose a layer which does not contain a form.
  // Then: The feature map pin is displayed on the map screen. Tapping on the map pin displays the
  // feature details.
  @Test
  @Ignore("flaky behavior on GCB")
  public void addFeatureWithNoForm() throws InterruptedException {
    dataBindingIdlingResource.monitorActivity(scenarioRule.getScenario());

    // Tap on the checkbox
    onView(withId(R.id.agreeCheckBox)).perform(click());

    // Tap on Submit on Terms Fragment
    onView(withId(R.id.agreeButton)).perform(click());

    // Tap on the "Add feature" button.
    onView(withId(R.id.add_feature_btn)).perform(click());

    // Tap on the layer type.
    onData(allOf(is(instanceOf(String.class)), is(FakeData.LAYER.getName())))
        .perform(click());

    // Tap on the crosshair at the centre of the map.
    onView(withId(R.id.map_crosshairs_img)).perform(click());

    // TODO: figure out how to remove this.
    //  See here for more: https://github.com/dturner/ground-android/pull/1
    Thread.sleep(10);

    // Verify that the feature title matches the layer title and that it is displayed.
    onView(withId(R.id.feature_title)).check(matches(isCompletelyDisplayed()));
    onView(withId(R.id.feature_title))
        .check(matches(withText(FakeData.LAYER.getName())));
  }
}
