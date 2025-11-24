package org.groundplatform.android.ui.home.mapcontainer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.ui.components.MapFloatingActionButton
import org.groundplatform.android.ui.components.MapFloatingActionButtonType
import org.groundplatform.android.ui.home.mapcontainer.jobs.AdHocDataCollectionButtonData
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponent
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentAction
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentState
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun BaseMapScreen(
  modifier: Modifier = Modifier,
  isLocationLocked: Boolean,
  shouldShowMapActions: Boolean,
  jobComponentState: JobMapComponentState,
  onBaseMapAction: (BaseMapAction) -> Unit,
  onJobComponentAction: (JobMapComponentAction) -> Unit,
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (shouldShowMapActions) {
      MapFloatingActionButton(
        modifier =
          Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .align(Alignment.TopStart),
        type = MapFloatingActionButtonType.OpenNavDrawer(),
        onClick = { onBaseMapAction(BaseMapAction.OnOpenNavDrawerClicked) },
      )

      MapFloatingActionButton(
        modifier =
          Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .align(Alignment.TopEnd),
        type = MapFloatingActionButtonType.MapType(),
        onClick = { onBaseMapAction(BaseMapAction.OnMapTypeClicked) },
      )
    }

    Column(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (shouldShowMapActions) {
        MapFloatingActionButton(
          modifier = Modifier.align(Alignment.End),
          type =
            if (isLocationLocked) MapFloatingActionButtonType.LocationLocked()
            else MapFloatingActionButtonType.LocationNotLocked(),
          onClick = { onBaseMapAction(BaseMapAction.OnLocationLockClicked) },
        )
      }

      JobMapComponent(
        modifier = Modifier,
        state = jobComponentState,
        onAction = onJobComponentAction,
      )
    }
  }
}

sealed interface BaseMapAction {
  data object OnMapTypeClicked : BaseMapAction

  data object OnLocationLockClicked : BaseMapAction

  data object OnOpenNavDrawerClicked : BaseMapAction
}

@Suppress("UnusedPrivateMember")
@Preview(showSystemUi = true)
@Composable
private fun BaseMapScreenPreview() {
  AppTheme {
    BaseMapScreen(
      isLocationLocked = false,
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
      onBaseMapAction = {},
      onJobComponentAction = {},
    )
  }
}
