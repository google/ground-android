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
package com.google.android.ground.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.GravityCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.BuildConfig
import com.google.android.ground.MainViewModel
import com.google.android.ground.R
import com.google.android.ground.databinding.HomeScreenFragBinding
import com.google.android.ground.databinding.NavDrawerHeaderBinding
import com.google.android.ground.model.User
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.BackPressListener
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.LocationOfInterestHelper
import com.google.android.ground.ui.theme.AppTheme
import com.google.android.ground.util.systemInsets
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Fragment containing the map container and location of interest sheet fragments and NavigationView
 * side drawer. This is the default view in the application, and gets swapped out for other
 * fragments (e.g., view submission and edit submission) at runtime.
 */
@AndroidEntryPoint
class HomeScreenFragment :
  AbstractFragment(), BackPressListener, NavigationView.OnNavigationItemSelectedListener {

  // TODO: It's not obvious which locations of interest are in HomeScreen vs MapContainer;
  //  make this more intuitive.

  @Inject lateinit var locationOfInterestHelper: LocationOfInterestHelper
  @Inject lateinit var locationOfInterestRepository: LocationOfInterestRepository
  @Inject lateinit var popups: EphemeralPopups
  @Inject lateinit var userRepository: UserRepository
  @Inject lateinit var surveyRepository: SurveyRepository

  private lateinit var binding: HomeScreenFragBinding
  private lateinit var homeScreenViewModel: HomeScreenViewModel
  private lateinit var user: User

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    getViewModel(MainViewModel::class.java).windowInsets.observe(this) { onApplyWindowInsets(it) }
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)
    lifecycleScope.launch { homeScreenViewModel.openDrawerRequestsFlow.collect { openDrawer() } }
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
    binding.versionText.text = String.format(getString(R.string.build), BuildConfig.VERSION_NAME)
    // Ensure nav drawer cannot be swiped out, which would conflict with map pan gestures.
    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    homeScreenViewModel.showOfflineAreaMenuItem.observe(viewLifecycleOwner) {
      binding.navView.menu.findItem(R.id.nav_offline_areas).isEnabled = it
    }

    binding.navView.setNavigationItemSelectedListener(this)
    val navHeader = binding.navView.getHeaderView(0)
    navHeader.findViewById<TextView>(R.id.switch_survey_button).setOnClickListener {
      homeScreenViewModel.showSurveySelector()
    }
    viewLifecycleOwner.lifecycleScope.launch { user = userRepository.getAuthenticatedUser() }
    navHeader.findViewById<ShapeableImageView>(R.id.user_image).setOnClickListener {
      showSignOutConfirmationDialogs()
    }
    updateNavHeader()
    // Re-open data collection screen if any drafts are present
    viewLifecycleOwner.lifecycleScope.launch {
      homeScreenViewModel.maybeNavigateToDraftSubmission()
    }
  }

  private fun updateNavHeader() =
    lifecycleScope.launch {
      val navHeader = binding.navView.getHeaderView(0)
      val headerBinding = NavDrawerHeaderBinding.bind(navHeader)
      headerBinding.user = userRepository.getAuthenticatedUser()
      surveyRepository.activeSurveyFlow.collect {
        if (it == null) {
          headerBinding.surveyInfo.visibility = View.GONE
          headerBinding.noSurveysInfo.visibility = View.VISIBLE
        } else {
          headerBinding.noSurveysInfo.visibility = View.GONE
          headerBinding.surveyInfo.visibility = View.VISIBLE
          headerBinding.survey = it
        }
      }
    }

  private fun openDrawer() {
    binding.drawerLayout.openDrawer(GravityCompat.START)
  }

  private fun closeDrawer() {
    binding.drawerLayout.closeDrawer(GravityCompat.START)
  }

  private fun onApplyWindowInsets(insets: WindowInsetsCompat) {
    val headerView = binding.navView.getHeaderView(0)
    headerView.setPadding(0, insets.systemInsets().top, 0, 0)
  }

  override fun onBack(): Boolean = false

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.sync_status -> homeScreenViewModel.showSyncStatus()
      R.id.nav_offline_areas -> homeScreenViewModel.showOfflineAreas()
      R.id.nav_settings -> homeScreenViewModel.showSettings()
      R.id.about -> homeScreenViewModel.showAbout()
      R.id.terms_of_service -> homeScreenViewModel.showTermsOfService()
    }
    closeDrawer()
    return true
  }

  private fun showSignOutConfirmationDialogs() {
    // Note: Adding a compose view to the fragment's view dynamically causes the navigation click to
    // stop working after 1st time. Revisit this once the navigation drawer is also generated using
    // compose.
    binding.composeView.apply {
      setContent {
        val showUserDetailsDialog = remember { mutableStateOf(true) }
        val showSignOutDialog = remember { mutableStateOf(false) }

        // reset the state for recomposition
        showUserDetailsDialog.value = true
        showSignOutDialog.value = false

        AppTheme {
          if (showUserDetailsDialog.value) {
            UserDetailsDialog(showUserDetailsDialog, showSignOutDialog, user)
          }
          if (showSignOutDialog.value) {
            SignOutConfirmationDialog(showUserDetailsDialog, showSignOutDialog) {
              userRepository.signOut()
            }
          }
        }
      }
    }
  }
}
