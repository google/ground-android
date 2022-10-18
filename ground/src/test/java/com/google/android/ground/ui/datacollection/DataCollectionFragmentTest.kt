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
package com.google.android.ground.ui.datacollection

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.viewpager2.widget.ViewPager2
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.rx.Loadable
import com.google.android.ground.ui.common.Navigator
import com.google.common.truth.Truth
import com.jraska.livedata.TestObserver
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Single
import javax.inject.Inject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DataCollectionFragmentTest : BaseHiltTest() {

  //  @Inject lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject lateinit var navigator: Navigator
  //  @Inject lateinit var viewModelFactory: ViewModelFactory
  @BindValue @Mock lateinit var surveyRepository: SubmissionRepository

  @Before
  override fun setUp() {
    super.setUp()

    whenever(
        surveyRepository.createSubmission(
          DataCollectionTestData.surveyId,
          DataCollectionTestData.loiId,
          DataCollectionTestData.submissionId
        )
      )
      .thenReturn(Single.just(DataCollectionTestData.submission))
    //    whenever(dataCollectionViewModel.submission)
    //      .doReturn(MutableLiveData(Loadable.loaded(DataCollectionTestData.submission)))
    //    submissionTestObserver = TestObserver.test(dataCollectionViewModel.submission)
    //
    //    val args = DataCollectionTestData.args.toBundle()
    //    launchFragmentInHiltContainer<DataCollectionFragment>(args)
  }

  @Test
  fun created_submissionIsLoaded() {
    //      dataCollectionViewModel.submission.test
    //      verify(dataCollectionViewModel).loadSubmissionDetails(eq(DataCollectionTestData.args))

    //   val submissionTestObserver = TestObserver.test(dataCollectionViewModel.submission)

    val args = DataCollectionTestData.args.toBundle()
    launchFragmentInHiltContainer<DataCollectionFragment>(args) {
      //      val dataCollectionViewModel = viewModelFactory.get(this,
      // DataCollectionViewModel::class.java)
      val dataCollectionViewModel = (this as DataCollectionFragment).viewModel
      val submissionTestObserver = TestObserver.test(dataCollectionViewModel.submission)
      submissionTestObserver.assertValue(Loadable.loaded(DataCollectionTestData.submission))
    }

    //    Truth.assertThat(submissionTestObserver.awaitValue().value()).isNull()
    //    submissionTestObserver.assertNoValue()
    //    assertThat(dataCollectionViewModel.submission.getOrAwaitValue())
    //      .isEqualTo(DataCollectionTestData.loiName)
  }

  @Test
  fun created_submissionIsLoaded_viewPagerAdapterIsSet() {
    val args = DataCollectionTestData.args.toBundle()
    launchFragmentInHiltContainer<DataCollectionFragment>(args)

    onView(withId(R.id.pager)).check(matches(isDisplayed()))
  }

  @Test
  fun onNextClicked_viewPagerItemIsUpdated() {
    //      onView(withId(R.id.pager)).check(matches(isDisplayed()))
    //      val viewPager = dataCollectionFragment.view!!.findViewById<ViewPager2>(R.id.pager)
    //      assertThat(viewPager.currentItem).isEqualTo(0)
    //      dataCollectionFragment.view!!
    //        .findViewById<Button>(R.id.data_collection_continue_button)
    //        .performClick()
    //      assertThat(viewPager.currentItem).isEqualTo(1)
    val args = DataCollectionTestData.args.toBundle()
    launchFragmentInHiltContainer<DataCollectionFragment>(args) {
      onView(withId(R.id.data_collection_continue_button)).perform(click())

      val fragment = this as DataCollectionFragment

      //
      // fragment.view!!.findViewById<Button>(R.id.data_collection_continue_button).performClick()
      val viewPager = fragment.view!!.findViewById<ViewPager2>(R.id.pager)
      Truth.assertThat(viewPager.currentItem).isEqualTo(1)
    }
  }

  //
  //  @Test
  //  fun onNextClicked_thenOnBack_viewPagerItemIsUpdated() {
  //    val viewPager = dataCollectionFragment.view!!.findViewById<ViewPager2>(R.id.pager)
  //    assertThat(viewPager.currentItem).isEqualTo(0)
  //    dataCollectionFragment.view!!
  //      .findViewById<Button>(R.id.data_collection_continue_button)
  //      .performClick()
  //    assertThat(viewPager.currentItem).isEqualTo(1)
  //    assertThat(dataCollectionFragment.onBack()).isTrue()
  //    assertThat(viewPager.currentItem).isEqualTo(0)
  //  }
  //
  //  @Test
  //  fun onBack_firstViewPagerItem_returnsFalse() {
  //    assertThat(dataCollectionFragment.onBack()).isFalse()
  //  }
}
