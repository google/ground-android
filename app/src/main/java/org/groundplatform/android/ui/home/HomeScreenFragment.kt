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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.view.GravityCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.groundplatform.android.BuildConfig
import org.groundplatform.android.R
import org.groundplatform.android.data.local.room.converter.SubmissionDeltasConverter
import org.groundplatform.android.databinding.HomeScreenFragBinding
import org.groundplatform.android.model.User
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.BackPressListener
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.components.ConfirmationDialog
import org.groundplatform.android.ui.main.MainViewModel
import org.groundplatform.android.util.setComposableContent

/**
 * Fragment containing the map container and location of interest sheet fragments and NavigationView
 * side drawer. This is the default view in the application, and gets swapped out for other
 * fragments (e.g., view submission and edit submission) at runtime.
 */
@AndroidEntryPoint
class HomeScreenFragment : AbstractFragment(), BackPressListener {

  @Inject lateinit var ephemeralPopups: EphemeralPopups
  @Inject lateinit var userRepository: UserRepository
  private lateinit var binding: HomeScreenFragBinding
  private lateinit var homeScreenViewModel: HomeScreenViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    getViewModel(MainViewModel::class.java).windowInsets.observe(this) { onApplyWindowInsets(it) }
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
    // Ensure nav drawer cannot be swiped out, which would conflict with map pan gestures.
    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

    binding.composeView.setComposableContent {
      val showSignOutDialog =
        homeScreenViewModel.showSignOutDialog.collectAsStateWithLifecycle(false)

      LaunchedEffect(Unit) { homeScreenViewModel.openDrawerRequestsFlow.collect { openDrawer() } }

      if (showSignOutDialog.value) {
        ConfirmationDialog(
          title = R.string.sign_out_dialog_title,
          description = R.string.sign_out_dialog_body,
          confirmButtonText = R.string.sign_out,
          onConfirmClicked = { homeScreenViewModel.signOut() },
        )
      }
    }

    binding.drawerView.setComposableContent {
      val user by
        produceState<User?>(initialValue = null) { value = userRepository.getAuthenticatedUser() }
      val survey by
        homeScreenViewModel.surveyRepository.activeSurveyFlow.collectAsStateWithLifecycle()

      HomeDrawer(
        user = user,
        survey = survey,
        onSwitchSurvey = {
          findNavController()
            .navigate(
              HomeScreenFragmentDirections.actionHomeScreenFragmentToSurveySelectorFragment(false)
            )
        },
        onNavigateToOfflineAreas = {
          lifecycleScope.launch {
            if (homeScreenViewModel.getOfflineAreas().isEmpty())
              findNavController().navigate(HomeScreenFragmentDirections.showOfflineAreaSelector())
            else findNavController().navigate(HomeScreenFragmentDirections.showOfflineAreas())
          }
          closeDrawer()
        },
        onNavigateToSyncStatus = {
          findNavController().navigate(HomeScreenFragmentDirections.showSyncStatus())
          closeDrawer()
        },
        onNavigateToSettings = {
          findNavController()
            .navigate(HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity())
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
        onSignOut = { homeScreenViewModel.showSignOutDialog() },
        versionText = String.format(getString(R.string.build), BuildConfig.VERSION_NAME),
      )
    }

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

  private fun onApplyWindowInsets(insets: WindowInsetsCompat) {}

  override fun onBack(): Boolean = false
}
