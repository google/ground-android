/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.ui.datacollection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.viewpager2.widget.ViewPager2

/**
 * Hosts the ViewPager2 that renders individual task screens.
 *
 * @param uiState The current UI state.
 * @param fragment The fragment hosting this screen.
 */
@Composable
fun DataCollectionViewPager(uiState: DataCollectionUiState, fragment: DataCollectionFragment) {
  AndroidView(
    factory = { context ->
      ViewPager2(context).apply {
        isUserInputEnabled = false
        offscreenPageLimit = 1
      }
    },
    update = { viewPager ->
      if (uiState is DataCollectionUiState.Ready) {
        val currentAdapter = viewPager.adapter as? DataCollectionViewPagerAdapter
        if (currentAdapter == null || currentAdapter.tasks != uiState.tasks) {
          viewPager.adapter = fragment.viewPagerAdapterFactory.create(fragment, uiState.tasks)
        }
        viewPager.doOnLayout { viewPager.setCurrentItem(uiState.position.absoluteIndex, false) }
      }
    },
    modifier = Modifier.fillMaxSize(),
  )
}
