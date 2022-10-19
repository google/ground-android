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
package com.google.android.ground.ui.home.mapcontainer

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.TestObserver
import com.sharedtest.FakeData
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Flowable
import java8.util.Optional
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

// TODO: Add more test coverage
//  1. Switching survey should update list
//  2. Panning should emit values without having to resubscribe
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LoiCardSourceTest : BaseHiltTest() {

  @Mock lateinit var locationOfInterestRepository: LocationOfInterestRepository
  @Mock lateinit var surveyRepository: SurveyRepository

  private lateinit var loiCardSource: LoiCardSource
  private lateinit var loisTestObserver: TestObserver<List<LocationOfInterest>>

  @Before
  override fun setUp() {
    super.setUp()

    Mockito.`when`(surveyRepository.activeSurvey)
      .thenReturn(Flowable.just(Optional.of(TEST_SURVEY)))

    Mockito.`when`(locationOfInterestRepository.getLocationsOfInterestOnceAndStream(TEST_SURVEY))
      .thenReturn(Flowable.just(TEST_LOCATIONS_OF_INTEREST))

    loiCardSource = LoiCardSource(surveyRepository, locationOfInterestRepository)
    loisTestObserver = TestObserver.test(loiCardSource.locationsOfInterest)
  }

  @Test
  fun testLoiCards_whenBoundsNotAvailable_returnsNothing() {
    loisTestObserver.assertNoValue()
  }

  @Test
  fun testLoiCards_whenOutOfBounds_returnsEmptyList() {
    val southwest = LatLng(-60.0, -60.0)
    val northeast = LatLng(-50.0, -50.0)
    loiCardSource.onCameraBoundsUpdated(LatLngBounds(southwest, northeast))

    assertThat(loisTestObserver.awaitValue().value()).isEmpty()
  }

  @Test
  fun testLoiCards_whenSomeLOIsInsideBounds_returnsPartialList() {
    val southwest = LatLng(-20.0, -20.0)
    val northeast = LatLng(-10.0, -10.0)

    loiCardSource.onCameraBoundsUpdated(LatLngBounds(southwest, northeast))

    assertThat(loisTestObserver.value())
      .isEqualTo(listOf(TEST_POINT_OF_INTEREST_1, TEST_AREA_OF_INTEREST_1))
  }

  @Test
  fun testLoiCards_whenAllLOIsInsideBounds_returnsCompleteList() {
    val southwest = LatLng(-20.0, -20.0)
    val northeast = LatLng(20.0, 20.0)

    loiCardSource.onCameraBoundsUpdated(LatLngBounds(southwest, northeast))

    assertThat(loisTestObserver.value())
      .isEqualTo(
        listOf(
          TEST_POINT_OF_INTEREST_1,
          TEST_POINT_OF_INTEREST_2,
          TEST_POINT_OF_INTEREST_3,
          TEST_AREA_OF_INTEREST_1,
          TEST_AREA_OF_INTEREST_2
        )
      )
  }

  companion object {
    private val COORDINATE_1 = Coordinate(-20.0, -20.0)
    private val COORDINATE_2 = Coordinate(0.0, 0.0)
    private val COORDINATE_3 = Coordinate(20.0, 20.0)

    private val TEST_SURVEY = FakeData.SURVEY

    private val TEST_POINT_OF_INTEREST_1 = createPoint("1", COORDINATE_1)
    private val TEST_POINT_OF_INTEREST_2 = createPoint("2", COORDINATE_2)
    private val TEST_POINT_OF_INTEREST_3 = createPoint("3", COORDINATE_3)
    private val TEST_AREA_OF_INTEREST_1 =
      createPolygon("4", ImmutableList.of(COORDINATE_1, COORDINATE_2))
    private val TEST_AREA_OF_INTEREST_2 =
      createPolygon("5", ImmutableList.of(COORDINATE_2, COORDINATE_3))

    private val TEST_LOCATIONS_OF_INTEREST =
      ImmutableSet.of(
        TEST_POINT_OF_INTEREST_1,
        TEST_POINT_OF_INTEREST_2,
        TEST_POINT_OF_INTEREST_3,
        TEST_AREA_OF_INTEREST_1,
        TEST_AREA_OF_INTEREST_2
      )

    private fun createPoint(id: String, coordinate: Coordinate) =
      FakeData.LOCATION_OF_INTEREST.copy(
        id = id,
        geometry = Point(coordinate),
        surveyId = TEST_SURVEY.id
      )

    private fun createPolygon(id: String, coordinates: ImmutableList<Coordinate>) =
      FakeData.AREA_OF_INTEREST.copy(
        id = id,
        geometry = LineString(coordinates),
        surveyId = TEST_SURVEY.id
      )
  }
}
