/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.home.mapcontainer

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.map.MapType
import org.groundplatform.android.proto.Survey.DataSharingTerms
import org.groundplatform.android.ui.basemapselector.BasemapSelectorScreen
import org.groundplatform.android.ui.components.MapFloatingActionButton
import org.groundplatform.android.ui.components.MapFloatingActionButtonType
import org.groundplatform.android.ui.components.RecenterButton
import org.groundplatform.android.ui.home.DataSharingTermsDialog
import org.groundplatform.android.ui.home.mapcontainer.jobs.AdHocDataCollectionButtonData
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponent
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentAction
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentState
import org.groundplatform.android.ui.map.MapFragment
import org.groundplatform.android.ui.theme.AppTheme
import timber.log.Timber

@Composable
fun HomeScreenMapContainerScreen(
  modifier: Modifier = Modifier,
  locationLockButtonType: MapFloatingActionButtonType,
  shouldShowMapActions: Boolean,
  shouldShowRecenter: Boolean,
  jobComponentState: JobMapComponentState,
  dataSharingTerms: DataSharingTerms?,
  showMapTypeSelector: Boolean,
  mapTypes: List<MapType>,
  onBaseMapAction: (BaseMapAction) -> Unit,
  onJobComponentAction: (JobMapComponentAction) -> Unit,
  onTermsConsentGiven: () -> Unit = {},
  onTermsConsentDismissed: () -> Unit = {},
  onMapTypeSelectorDismiss: () -> Unit = {},
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (showMapTypeSelector) {
      BasemapSelectorScreen(mapTypes = mapTypes, onDismissRequest = onMapTypeSelectorDismiss)
    }

    if (dataSharingTerms != null) {
      DataSharingTermsDialog(
        dataSharingTerms = dataSharingTerms,
        onConfirm = onTermsConsentGiven,
        onDismiss = onTermsConsentDismissed,
      )
    }

    if (shouldShowMapActions) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
      ) {
        MapFloatingActionButton(
          modifier = Modifier.align(Alignment.TopStart),
          type = MapFloatingActionButtonType.OpenNavDrawer,
          onClick = { onBaseMapAction(BaseMapAction.OnOpenNavDrawerClicked) },
        )

        MapFloatingActionButton(
          modifier = Modifier.align(Alignment.TopEnd),
          type = MapFloatingActionButtonType.MapType,
          onClick = { onBaseMapAction(BaseMapAction.OnMapTypeClicked) },
        )
      }
    }

    Column(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (shouldShowMapActions) {
        LocationLockComponent(
          shouldShowRecenter = shouldShowRecenter,
          locationLockButtonType = locationLockButtonType,
          onAction = onBaseMapAction,
        )
      }

      JobMapComponent(state = jobComponentState, onAction = onJobComponentAction)
    }
  }
}

@Composable
private fun LocationLockComponent(
  modifier: Modifier = Modifier,
  shouldShowRecenter: Boolean,
  locationLockButtonType: MapFloatingActionButtonType,
  onAction: (BaseMapAction) -> Unit,
) {
  Row(
    modifier =
      modifier
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (shouldShowRecenter) {
      RecenterButton(onClick = { onAction(BaseMapAction.OnLocationLockClicked) })
    }

    Spacer(modifier = Modifier.weight(1f))

    MapFloatingActionButton(
      type = locationLockButtonType,
      onClick = { onAction(BaseMapAction.OnLocationLockClicked) },
    )
  }
}

sealed interface BaseMapAction {
  data object OnMapTypeClicked : BaseMapAction

  data object OnLocationLockClicked : BaseMapAction

  data object OnOpenNavDrawerClicked : BaseMapAction
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenMapContainerScreenPreview() {
  AppTheme {
    HomeScreenMapContainerScreen(
      locationLockButtonType = MapFloatingActionButtonType.LocationNotLocked,
      jobComponentState =
        JobMapComponentState(
          selectedLoi = null,
          adHocDataCollectionButtonData =
            listOf(
              AdHocDataCollectionButtonData(
                canCollectData = true,
                job =
                  Job(
                    id = "1",
                    style = Style(color = "#4169E1"),
                    name = "job 1",
                    tasks = emptyMap(),
                  ),
              )
            ),
        ),
      shouldShowMapActions = true,
      shouldShowRecenter = true,
      dataSharingTerms = null,
      showMapTypeSelector = false,
      mapTypes = listOf(MapType.ROAD, MapType.TERRAIN, MapType.SATELLITE),
      onBaseMapAction = {},
      onJobComponentAction = {},
    )
  }
}

@Composable
internal fun MapFrame(map: MapFragment, fragment: HomeScreenMapContainerFragment) {
  AndroidView(
    factory = { context ->
      android.widget.FrameLayout(context).apply {
        id = R.id.map
        layoutParams =
          android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
          )
      }
    },
    modifier = Modifier.fillMaxSize(),
    update = {
      val container = it
      val mapFragment = fragment.childFragmentManager.findFragmentById(R.id.map) as? MapFragment
      val currentMap = mapFragment ?: map
      if (map !== currentMap) {
        fragment.map = currentMap
      }

      val fragmentView = (mapFragment as? Fragment)?.view
      if (mapFragment == null || fragmentView == null || fragmentView.parent != container) {
        Timber.d("Attaching map fragment to container: $container")
        try {
          map.attachToParent(fragment, R.id.map) { fragment.onMapReadyPublic(it) }
        } catch (e: Exception) {
          Timber.e(e, "Failed to attach map fragment")
        }
      }
    },
  )
}

@Composable
internal fun HomeScreenMapContainerContent(
  map: MapFragment,
  mapContainerViewModel: HomeScreenMapContainerViewModel,
  bottomContainer: ViewGroup,
  fragment: HomeScreenMapContainerFragment,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    MapFrame(map, fragment)

    val locationLockButton by
      mapContainerViewModel.locationLockIconType.collectAsStateWithLifecycle()
    val jobMapComponentState by
      mapContainerViewModel.jobMapComponentState.collectAsStateWithLifecycle()
    val shouldShowMapActions by
      mapContainerViewModel.shouldShowMapActions.collectAsStateWithLifecycle()
    val shouldShowRecenter by
      mapContainerViewModel.shouldShowRecenterButton.collectAsStateWithLifecycle()
    val dataSharingTerms by mapContainerViewModel.dataSharingTerms.collectAsStateWithLifecycle()
    val showMapTypeSelector by
      mapContainerViewModel.showMapTypeSelector.collectAsStateWithLifecycle()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          mapContainerViewModel.navigateToDataCollectionFragment.collect {
            fragment.navigateToDataCollectionFragment(it)
          }
        }
        launch {
          mapContainerViewModel.termsError.collect {
            fragment.ephemeralPopups.ErrorPopup().show(fragment.getString(it))
          }
        }
      }
    }

    HomeScreenMapContainerScreen(
      locationLockButtonType = locationLockButton,
      shouldShowMapActions = shouldShowMapActions,
      shouldShowRecenter = shouldShowRecenter,
      jobComponentState = jobMapComponentState,
      dataSharingTerms = dataSharingTerms,
      showMapTypeSelector = showMapTypeSelector,
      mapTypes = map.supportedMapTypes,
      onBaseMapAction = { fragment.handleMapAction(it) },
      onJobComponentAction = {
        fragment.handleJobMapComponentAction(
          jobMapComponentState = jobMapComponentState,
          action = it,
        )
      },
      onTermsConsentGiven = { mapContainerViewModel.onTermsConsentGiven() },
      onTermsConsentDismissed = { mapContainerViewModel.onTermsConsentDismissed() },
      onMapTypeSelectorDismiss = { mapContainerViewModel.showMapTypeSelector.value = false },
    )

    AndroidView(factory = { bottomContainer }, modifier = Modifier.fillMaxSize())
  }
}
