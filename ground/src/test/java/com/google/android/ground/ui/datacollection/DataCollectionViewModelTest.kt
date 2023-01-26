package com.google.android.ground.ui.datacollection

import androidx.lifecycle.SavedStateHandle
import com.google.android.ground.BaseHiltTest
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DataCollectionViewModelTest : BaseHiltTest() {
  @Inject lateinit var dataCollectionViewModelFactory: DataCollectionViewModel.Factory

  @Test
  fun savedState_containsCurrentPosition_returnsCorrectCurrentPosition() {
    val currentPosition = 4
    val savedState = SavedStateHandle(mapOf("currentPosition" to currentPosition))
    val viewModel = dataCollectionViewModelFactory.create(savedState)

    assertThat(viewModel.currentPosition.value).isEqualTo(currentPosition)
  }
}
