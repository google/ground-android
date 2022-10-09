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
package com.google.android.ground.ui.tos

import androidx.core.os.bundleOf
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TermsOfServiceFragmentTest : BaseHiltTest() {

  @Inject lateinit var navigator: Navigator
  @Inject lateinit var termsOfServiceRepository: TermsOfServiceRepository

  @Test
  fun termsOfServiceText_shouldBeDisplayed() {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(
      bundleOf(Pair("termsOfServiceText", TEST_TOS_TEXT))
    )

    onView(withId(R.id.termsText))
      .check(matches(isDisplayed()))
      .check(matches(withText(TEST_TOS_TEXT)))
  }

  @Test
  fun agreeButton_default_shouldNotBeEnabled() {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf())

    onView(withId(R.id.agreeCheckBox)).check(matches(isNotChecked()))
    onView(withId(R.id.agreeButton)).check(matches(not(isEnabled())))
  }

  @Test
  fun agreeButton_whenCheckBoxClicked_shouldBeEnabled() {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf())

    onView(withId(R.id.agreeCheckBox)).perform(click()).check(matches(isChecked()))
    onView(withId(R.id.agreeButton)).check(matches(isEnabled()))
  }

  @Test
  fun agreeButton_whenPressed_shouldUpdatePrefAndNavigate() {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf())

    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isFalse()
    val navDirectionsTestObserver = navigator.getNavigateRequests().test()

    onView(withId(R.id.agreeCheckBox)).perform(click())
    onView(withId(R.id.agreeButton)).perform(click())

    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isTrue()
    navDirectionsTestObserver.assertValue(HomeScreenFragmentDirections.showHomeScreen())
  }

  @Test
  fun pressBack_shouldFinishActivity() {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf()) {
      val fragment = this as TermsOfServiceFragment
      assertThat(fragment.onBack()).isFalse()
      assertThat(activity?.isFinishing).isTrue()
    }
  }

  companion object {
    const val TEST_TOS_TEXT = "Sample terms of service"
  }
}
