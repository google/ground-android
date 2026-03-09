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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.GravityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.data.local.room.converter.SubmissionDeltasConverter
import org.groundplatform.android.databinding.HomeScreenFragBinding
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.BackPressListener
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.util.setComposableContent

/**
 * Fragment containing the map container and location of interest sheet fragments and NavigationView
 * side drawer. This is the default view in the application, and gets swapped out for other
 * fragments (e.g., view submission and edit submission) at runtime.
 */
@AndroidEntryPoint
class HomeScreenFragment : AbstractFragment(), BackPressListener {

  @Inject lateinit var ephemeralPopups: EphemeralPopups
  private lateinit var binding: HomeScreenFragBinding
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
    binding = HomeScreenFragBinding.inflate(inflater, container, false)
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val binding = binding

    setupComposeView(binding)
    setupDrawerContent(binding)
    restoreDraftSubmission(binding)
  }

  private fun setupComposeView(binding: HomeScreenFragBinding) {
    binding.composeView.setComposableContent {
      LaunchedEffect(Unit) { homeScreenViewModel.openDrawerRequestsFlow.collect { openDrawer() } }
      SetupUserConfirmationDialog()
    }
  }

  private fun setupDrawerContent(binding: HomeScreenFragBinding) {
    binding.drawerView.setComposableContent {
      val drawerState by homeScreenViewModel.drawerState.collectAsStateWithLifecycle()

      drawerState?.let { state ->
        HomeDrawer(
          user = state.user,
          survey = state.survey,
          versionText = String.format(getString(R.string.build), state.appVersion),
          onAction = { action ->
            when (action) {
              HomeDrawerAction.OnSwitchSurvey -> {
                findNavController()
                  .navigate(
                    HomeScreenFragmentDirections.actionHomeScreenFragmentToSurveySelectorFragment(
                      false
                    )
                  )
              }
              HomeDrawerAction.OnNavigateToOfflineAreas -> {
                lifecycleScope.launch {
                  if (homeScreenViewModel.getOfflineAreas().isEmpty())
                    findNavController()
                      .navigate(HomeScreenFragmentDirections.showOfflineAreaSelector())
                  else findNavController().navigate(HomeScreenFragmentDirections.showOfflineAreas())
                }
                closeDrawer()
              }
              HomeDrawerAction.OnNavigateToSyncStatus -> {
                findNavController().navigate(HomeScreenFragmentDirections.showSyncStatus())
                closeDrawer()
              }
              HomeDrawerAction.OnNavigateToSettings -> {
                findNavController()
                  .navigate(
                    HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity()
                  )
                closeDrawer()
              }
              HomeDrawerAction.OnNavigateToAbout -> {
                findNavController().navigate(HomeScreenFragmentDirections.showAbout())
                closeDrawer()
              }
              HomeDrawerAction.OnNavigateToTerms -> {
                findNavController().navigate(HomeScreenFragmentDirections.showTermsOfService(true))
                closeDrawer()
              }
              HomeDrawerAction.OnSignOut -> {
                homeScreenViewModel.showSignOutConfirmation()
              }
              HomeDrawerAction.OnUserDetails -> {
                homeScreenViewModel.showUserDetails()
              }
            }
          },
        )
      }
    }
  }

  private fun restoreDraftSubmission(binding: HomeScreenFragBinding) {
    // Re-open data collection screen if draft submission is present.
    viewLifecycleOwner.lifecycleScope.launch {
      homeScreenViewModel.getDraftSubmission()?.let { draft ->
        findNavController()
          .navigate(
            HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
              draft.loiId,
              draft.loiName ?: "",
              draft.jobId,
              true,
              SubmissionDeltasConverter.toString(draft.deltas),
              draft.currentTaskId ?: "",
            )
          )

        if (!homeScreenViewModel.awaitingPhotoCapture) {
          ephemeralPopups
            .InfoPopup(binding.root, R.string.draft_restored, EphemeralPopups.PopupDuration.SHORT)
            .show()
        } else {
          // We're restoring after an instantaneous photo capture for a photo task; don't show a
          // draft restored toast.
          homeScreenViewModel.awaitingPhotoCapture = false
        }
      }
    }
  }

  private fun openDrawer() {
    binding.drawerLayout.openDrawer(GravityCompat.START)
  }

  private fun closeDrawer() {
    binding.drawerLayout.closeDrawer(GravityCompat.START)
  }

  override fun onBack(): Boolean = false

  @Composable
  private fun SetupUserConfirmationDialog() {
    val state by homeScreenViewModel.accountDialogState.collectAsStateWithLifecycle()
    val user by homeScreenViewModel.user.collectAsStateWithLifecycle(null)

    UserAccountDialogs(
      state = state,
      user = user,
      onSignOut = { homeScreenViewModel.signOut() },
      onShowSignOutConfirmation = { homeScreenViewModel.showSignOutConfirmation() },
      onDismiss = { homeScreenViewModel.dismissLogoutDialog() },
    )
  }
}
