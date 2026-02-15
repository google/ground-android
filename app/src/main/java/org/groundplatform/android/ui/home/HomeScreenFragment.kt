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
import android.view.Gravity
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
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
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
  private lateinit var drawerLayout: DrawerLayout

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
    
    drawerLayout = DrawerLayout(requireContext())
    drawerLayout.layoutParams =
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )

    // -------------------------------------------------------------------------
    // 1. Content View (FrameLayout)
    //    Contains: Map (FragmentContainerView) + Overlays (ComposeView)
    // -------------------------------------------------------------------------
    val contentRoot = FrameLayout(requireContext())
    contentRoot.layoutParams =
      DrawerLayout.LayoutParams(
        DrawerLayout.LayoutParams.MATCH_PARENT,
        DrawerLayout.LayoutParams.MATCH_PARENT
      )

    // 1a. Map Container
    val mapContainer = FragmentContainerView(requireContext())
    mapContainer.id = R.id.map_container_fragment
    mapContainer.layoutParams =
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
    contentRoot.addView(mapContainer)

    // 1b. Overlays (Dialogs, etc.)
    val overlaysView = ComposeView(requireContext())
    overlaysView.layoutParams =
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
    contentRoot.addView(overlaysView)

    drawerLayout.addView(contentRoot)

    // -------------------------------------------------------------------------
    // 2. Drawer View (ComposeView)
    // -------------------------------------------------------------------------
    val drawerView = ComposeView(requireContext())
    val drawerParams =
      DrawerLayout.LayoutParams(
        DrawerLayout.LayoutParams.WRAP_CONTENT,
        DrawerLayout.LayoutParams.MATCH_PARENT
      )
    drawerParams.gravity = Gravity.START
    drawerView.layoutParams = drawerParams
    drawerLayout.addView(drawerView)

    // -------------------------------------------------------------------------
    // Logic setup
    // -------------------------------------------------------------------------
    
    // Ensure the map fragment is added if not present
    if (childFragmentManager.findFragmentById(R.id.map_container_fragment) == null) {
      val mapFragment =
        org.groundplatform.android.ui.home.mapcontainer.HomeScreenMapContainerFragment()
      childFragmentManager
        .beginTransaction()
        .replace(R.id.map_container_fragment, mapFragment)
        .commit()
    }

    // Set content for Drawer
    drawerView.setContent {
      org.groundplatform.android.ui.theme.AppTheme {
        val user by
          androidx.compose.runtime.produceState<User?>(initialValue = null) {
            value = userRepository.getAuthenticatedUser()
          }
        val survey by
          homeScreenViewModel.surveyRepository.activeSurveyFlow.collectAsStateWithLifecycle(null)
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val offlineAreasEnabled by homeScreenViewModel.showOfflineAreaMenuItem.observeAsState(true)

        // Close drawer callback
        val closeDrawer: () -> Unit = {
           scope.launch { drawerLayout.closeDrawer(GravityCompat.START) }
        }

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
            closeDrawer()
          },
          onNavigateToOfflineAreas = {
            if (offlineAreasEnabled) {
              scope.launch {
                if (homeScreenViewModel.getOfflineAreas().isEmpty())
                  findNavController()
                    .navigate(HomeScreenFragmentDirections.showOfflineAreaSelector())
                else
                  findNavController().navigate(HomeScreenFragmentDirections.showOfflineAreas())
                closeDrawer()
              }
            }
          },
          onNavigateToSyncStatus = {
            findNavController().navigate(HomeScreenFragmentDirections.showSyncStatus())
            closeDrawer()
          },
          onNavigateToSettings = {
            findNavController()
              .navigate(
                HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity()
              )
            closeDrawer()
          },
          onNavigateToAbout = {
            findNavController().navigate(HomeScreenFragmentDirections.showAbout())
            closeDrawer()
          },
          onNavigateToTerms = {
            findNavController().navigate(HomeScreenFragmentDirections.showTermsOfService(true))
            closeDrawer()
          },
          onSignOut = {
             homeScreenViewModel.showSignOutDialog()
             closeDrawer()
          },
          versionText =
            String.format(
              getString(R.string.build),
              org.groundplatform.android.BuildConfig.VERSION_NAME,
            ),
        )
      }
    }

    // Set content for Overlays
    overlaysView.setContent {
      org.groundplatform.android.ui.theme.AppTheme {
        // Handle open drawer requests
        androidx.compose.runtime.LaunchedEffect(Unit) {
          homeScreenViewModel.openDrawerRequestsFlow.collect { 
            drawerLayout.openDrawer(GravityCompat.START) 
          }
        }

        val showSignOutDialog = remember { mutableStateOf(false) }
        
        androidx.compose.runtime.LaunchedEffect(Unit) {
          homeScreenViewModel.showSignOutDialog.collect {
             showSignOutDialog.value = true
          }
        }
        
        HomeScreenContent(
          homeScreenViewModel = homeScreenViewModel,
          ephemeralPopups = ephemeralPopups,
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
      }
    }
    
    ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, insets ->
        // Return insets unconsumed so children (ComposeView, etc.) receive them.
        insets
    }
    
    return drawerLayout
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onBack(): Boolean {
    if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.START)) {
      drawerLayout.closeDrawer(GravityCompat.START)
      return true
    }
    return false
  }
}

@Composable
private fun HomeScreenContent(
  homeScreenViewModel: HomeScreenViewModel,
  ephemeralPopups: EphemeralPopups,
  showSignOutDialog: androidx.compose.runtime.MutableState<Boolean>,
  onNavigateToDataCollection: (String, String?, String, Boolean, String?, String) -> Unit,
) {
  val view = androidx.compose.ui.platform.LocalView.current

  // Sign Out Confirmation
  if (showSignOutDialog.value) {
    ConfirmationDialog(
      title = R.string.sign_out_dialog_title,
      description = R.string.sign_out_dialog_body,
      confirmButtonText = R.string.sign_out,
      onConfirmClicked = {
        homeScreenViewModel.signOut()
        showSignOutDialog.value = false
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

