/*
 * Copyright 2018 Google LLC
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
package org.groundplatform.android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.data.local.room.converter.SubmissionDeltasConverter
import org.groundplatform.android.model.User
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.BackPressListener
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.components.ConfirmationDialog

/**
 * Fragment containing the map container and location of interest sheet fragments and NavigationView
 * side drawer. This is the default view in the application, and gets swapped out for other
 * fragments (e.g., view submission and edit submission) at runtime.
 */
@AndroidEntryPoint
class HomeScreenFragment : AbstractFragment(), BackPressListener {

  @Inject lateinit var ephemeralPopups: EphemeralPopups
  @Inject lateinit var userRepository: UserRepository

  private lateinit var homeScreenViewModel: HomeScreenViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    
    val root = FrameLayout(requireContext())
    root.layoutParams =
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )

    // 1. Map Container (FragmentContainerView) - Added first (bottom layer)
    // We use a predefined ID if possible, or generate one. 
    // Ideally we usage R.id.map_container_fragment if it exists (checked previously).
    val mapContainer = FragmentContainerView(requireContext())
    mapContainer.id = R.id.map_container_fragment
    mapContainer.layoutParams =
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
    root.addView(mapContainer)

    // 2. Compose Overlay - Added second (top layer)
    val composeView = ComposeView(requireContext())
    composeView.layoutParams =
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
    
    // Ensure the map fragment is added if not present
    if (childFragmentManager.findFragmentById(R.id.map_container_fragment) == null) {
      val mapFragment =
        org.groundplatform.android.ui.home.mapcontainer.HomeScreenMapContainerFragment()
      childFragmentManager
        .beginTransaction()
        .replace(R.id.map_container_fragment, mapFragment)
        .commit()
    }

    composeView.setContent {
      org.groundplatform.android.ui.theme.AppTheme {
        val drawerState =
          androidx.compose.material3.rememberDrawerState(
            androidx.compose.material3.DrawerValue.Closed
          )
        val user by
          androidx.compose.runtime.produceState<User?>(initialValue = null) {
            value = userRepository.getAuthenticatedUser()
          }
        val survey by
          homeScreenViewModel.surveyRepository.activeSurveyFlow.collectAsStateWithLifecycle(null)
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        val offlineAreasEnabled by homeScreenViewModel.showOfflineAreaMenuItem.observeAsState(true)

        // Handle open drawer requests from ViewModel
        androidx.compose.runtime.LaunchedEffect(Unit) {
          homeScreenViewModel.openDrawerRequestsFlow.collect { drawerState.open() }
        }

        val showSignOutDialog = remember { mutableStateOf(false) }

        androidx.activity.compose.BackHandler(enabled = drawerState.isOpen) {
          scope.launch { drawerState.close() }
        }

        HomeScreen(
          drawerState = drawerState,
          drawerContent = {
            HomeDrawer(
              user = user,
              survey = survey,
              onSwitchSurvey = {
                findNavController()
                  .navigate(
                    HomeScreenFragmentDirections.actionHomeScreenFragmentToSurveySelectorFragment(
                      false
                    )
                  )
                scope.launch { drawerState.close() }
              },
              onNavigateToOfflineAreas = {
                if (offlineAreasEnabled) {
                  scope.launch {
                    if (homeScreenViewModel.getOfflineAreas().isEmpty())
                      findNavController()
                        .navigate(HomeScreenFragmentDirections.showOfflineAreaSelector())
                    else
                      findNavController().navigate(HomeScreenFragmentDirections.showOfflineAreas())
                    drawerState.close()
                  }
                }
              },
              onNavigateToSyncStatus = {
                findNavController().navigate(HomeScreenFragmentDirections.showSyncStatus())
                scope.launch { drawerState.close() }
              },
              onNavigateToSettings = {
                findNavController()
                  .navigate(
                    HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity()
                  )
                scope.launch { drawerState.close() }
              },
              onNavigateToAbout = {
                findNavController().navigate(HomeScreenFragmentDirections.showAbout())
                scope.launch { drawerState.close() }
              },
              onNavigateToTerms = {
                findNavController().navigate(HomeScreenFragmentDirections.showTermsOfService(true))
                scope.launch { drawerState.close() }
              },
              onSignOut = { showSignOutDialog.value = true },
              versionText =
                String.format(
                  getString(R.string.build),
                  org.groundplatform.android.BuildConfig.VERSION_NAME,
                ),
            )
          },
          content = {
            // Pass empty content or transparent content here if HomeScreen draws a background.
            // If HomeScreen uses ModalNavigationDrawer, it puts `content` behind the drawer.
            // We want the map to be visible.
            // If the `HomeScreen` (ModalNavigationDrawer) has a transparent background for `content`, it should work.
            // However, `AppTheme` might set a background color on the root Surface.
            // We need to ensure this part is transparent.
            // We put the UI overlays that sit ON TOP of the map here.
            HomeScreenContent(
              homeScreenViewModel = homeScreenViewModel,
              ephemeralPopups = ephemeralPopups,
              drawerState = drawerState,
              showSignOutDialog = showSignOutDialog,
              onNavigateToDataCollection = { loiId, loiName, jobId, restore, deltas, taskId ->
                findNavController()
                  .navigate(
                    HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
                      loiId,
                      loiName,
                      jobId,
                      restore,
                      deltas,
                      taskId,
                    )
                  )
              },
            )
          },
        )
      }
    }
    
    root.addView(composeView)
    
    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        WindowInsetsCompat.CONSUMED
    }
    
    return root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onBack(): Boolean = false
}

@Composable
private fun HomeScreenContent(
  homeScreenViewModel: HomeScreenViewModel,
  ephemeralPopups: EphemeralPopups,
  drawerState: androidx.compose.material3.DrawerState,
  showSignOutDialog: androidx.compose.runtime.MutableState<Boolean>,
  onNavigateToDataCollection: (String, String?, String, Boolean, String?, String) -> Unit,
) {
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  val view = androidx.compose.ui.platform.LocalView.current

  // Map Container removed from here as it is now in the Fragment hierarchy.

  // Sign Out Confirmation
  if (showSignOutDialog.value) {
    ConfirmationDialog(
      title = R.string.sign_out_dialog_title,
      description = R.string.sign_out_dialog_body,
      confirmButtonText = R.string.sign_out,
      onConfirmClicked = {
        homeScreenViewModel.signOut()
        showSignOutDialog.value = false
        scope.launch { drawerState.close() }
      },
      onDismiss = { showSignOutDialog.value = false },
    )
  }

  androidx.compose.runtime.LaunchedEffect(Unit) {
    homeScreenViewModel.getDraftSubmission()?.let { draft ->
      onNavigateToDataCollection(
        draft.loiId ?: "",
        draft.loiName,
        draft.jobId,
        true,
        SubmissionDeltasConverter.toString(draft.deltas),
        draft.currentTaskId ?: "",
      )
      if (!homeScreenViewModel.awaitingPhotoCapture) {
        ephemeralPopups
          .InfoPopup(view, R.string.draft_restored, EphemeralPopups.PopupDuration.SHORT)
          .show()
      } else {
        homeScreenViewModel.awaitingPhotoCapture = false
      }
    }
  }
}
