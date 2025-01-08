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

import android.text.Html
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.android.ground.launchFragmentWithNavController
import com.google.android.ground.model.TermsOfService
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.system.NetworkManager
import com.google.common.truth.Truth.assertThat
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Suppress("StringShouldBeRawString")
class TermsOfServiceFragmentTest : BaseHiltTest() {

  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var termsOfServiceRepository: TermsOfServiceRepository
  @Inject lateinit var viewModel: TermsOfServiceViewModel
  private lateinit var navController: NavController

  @BindValue @Mock lateinit var networkManager: NetworkManager

  private fun withHtml(html: String): Matcher<View> =
    object : BaseMatcher<View>() {
      override fun describeTo(description: Description?) {
        description?.apply { this.appendText(html) }
      }

      override fun matches(actual: Any?): Boolean {
        val textView = actual as TextView
        return Html.toHtml(SpannableStringBuilder(textView.text), 0) == html
      }

      override fun describeMismatch(item: Any?, description: Description?) {
        description?.appendText(Html.toHtml(SpannableStringBuilder((item as TextView).text), 0))
        super.describeMismatch(item, description)
      }
    }

  override fun setUp() {
    super.setUp()
    fakeRemoteDataStore.termsOfService = Result.success(TEST_TOS)
  }

  @Test
  fun termsOfServiceText_shouldBeDisplayed() {
    whenever(networkManager.isNetworkConnected()).thenReturn(true)
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf(Pair("isViewOnly", false)))

    onView(withId(R.id.termsText))
      .check(matches(isDisplayed()))
      .check(matches(withText("This is a heading\n\nSample terms of service\n\n")))
      .check(
        matches(
          withHtml(
            "<p dir=\"ltr\"><span style=\"font-size:1.50em;\"><b>This is a heading</b></span></p>\n" +
              "<p dir=\"ltr\">Sample terms of service</p>\n"
          )
        )
      )
  }

  @Test
  fun agreeButton_default_shouldNotBeEnabled() {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf(Pair("isViewOnly", false)))

    onView(withId(R.id.agreeCheckBox)).check(matches(isNotChecked()))
    onView(withId(R.id.agreeButton)).check(matches(not(isEnabled())))
  }

  @Test
  fun agreeButton_whenCheckBoxClicked_shouldBeEnabled() {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf(Pair("isViewOnly", false)))

    onView(withId(R.id.agreeCheckBox)).perform(click()).check(matches(isChecked()))
    onView(withId(R.id.agreeButton)).check(matches(isEnabled()))
  }

  @Test
  fun agreeButton_whenPressed_shouldUpdatePrefAndNavigate() = runWithTestDispatcher {
    launchFragmentWithNavController<TermsOfServiceFragment>(
      fragmentArgs = bundleOf(Pair("isViewOnly", false)),
      destId = R.id.terms_of_service_fragment,
      navControllerCallback = { navController = it },
    )

    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isFalse()

    onView(withId(R.id.agreeCheckBox)).perform(click())
    onView(withId(R.id.agreeButton)).perform(click())

    assertThat(navController.currentDestination?.id).isEqualTo(R.id.surveySelectorFragment)

    assertThat(termsOfServiceRepository.isTermsOfServiceAccepted).isTrue()
  }

  @Test
  fun viewOnlyMode_controlsHidden() = runWithTestDispatcher {
    launchFragmentInHiltContainer<TermsOfServiceFragment>(bundleOf(Pair("isViewOnly", true)))

    onView(withId(R.id.agreeCheckBox)).check(matches(not(isDisplayed())))
    onView(withId(R.id.agreeButton)).check(matches(not(isDisplayed())))
  }

  companion object {
    const val TEST_TOS_TEXT = "# This is a heading\n\nSample terms of service"
    val TEST_TOS = TermsOfService("TERMS_OF_SERVICE", TEST_TOS_TEXT)
  }
}
