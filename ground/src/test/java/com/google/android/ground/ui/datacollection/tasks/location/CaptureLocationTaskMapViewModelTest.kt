package com.google.android.ground.ui.datacollection.tasks.location

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CaptureLocationTaskMapViewModelTest : BaseHiltTest() {

  @Inject lateinit var captureLocationTaskMapViewModel: CaptureLocationTaskMapViewModel
  @Inject lateinit var baseMapViewModel: BaseMapViewModel

  @Test
  fun testGetMapConfig() {
    assertThat(captureLocationTaskMapViewModel.mapConfig)
      .isEqualTo(baseMapViewModel.mapConfig.copy(allowGestures = false))
  }
}
