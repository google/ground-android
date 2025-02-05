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
package com.google.android.ground

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.google.android.ground.databinding.MainActBinding
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.ActivityCallback
import com.google.android.ground.system.ActivityStreams
import com.google.android.ground.ui.common.BackPressListener
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.common.modalSpinner
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.ui.signin.SignInFragmentDirections
import com.google.android.ground.ui.surveyselector.SurveySelectorFragmentDirections
import com.google.android.ground.util.renderComposableDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The app's main activity. The app consists of multiples Fragments that live under this activity.
 */
@AndroidEntryPoint
class MainActivity : AbstractActivity() {
  @Inject lateinit var activityStreams: ActivityStreams
  @Inject lateinit var viewModelFactory: ViewModelFactory
  @Inject lateinit var userRepository: UserRepository

  private lateinit var viewModel: MainViewModel
  private lateinit var navHostFragment: NavHostFragment

  private var signInProgressDialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    // Make sure this is before calling super.onCreate()
    setTheme(R.style.AppTheme)
    // TODO: Remove this to enable dark theme.
    // Issue URL: https://github.com/google/ground-android/issues/620
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    super.onCreate(savedInstanceState)

    // Set up event streams first. Navigator must be listening when auth is first initialized.
    lifecycleScope.launch {
      activityStreams.activityRequests.collect { callback: ActivityCallback ->
        callback(this@MainActivity)
      }
    }

    val binding = MainActBinding.inflate(layoutInflater)
    setContentView(binding.root)

    navHostFragment =
      supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

    viewModel = viewModelFactory[this, MainViewModel::class.java]

    lifecycleScope.launch { viewModel.navigationRequests.filterNotNull().collect { updateUi(it) } }

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (!dispatchBackPressed()) {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
          }
        }
      },
    )
  }

  private fun updateUi(uiState: MainUiState) {
    when (uiState) {
      MainUiState.OnPermissionDenied -> {
        showPermissionDeniedDialog()
      }
      MainUiState.OnUserSignedOut -> {
        navigateTo(SignInFragmentDirections.showSignInScreen())
      }
      MainUiState.TosNotAccepted -> {
        navigateTo(SignInFragmentDirections.showTermsOfService(false))
      }
      MainUiState.NoActiveSurvey -> {
        navigateTo(SurveySelectorFragmentDirections.showSurveySelectorScreen(true))
      }
      MainUiState.ShowHomeScreen -> {
        navigateTo(HomeScreenFragmentDirections.showHomeScreen())
      }
      MainUiState.OnUserSigningIn -> {
        onSignInProgress(true)
      }
    }

    if (uiState != MainUiState.OnUserSigningIn) {
      onSignInProgress(false)
    }
  }

  private fun showPermissionDeniedDialog() {
    renderComposableDialog {
      var showDialog by remember { mutableStateOf(true) }
      if (showDialog) {
        PermissionDeniedDialog(
          // TODO: Read url from Firestore config/properties/signUpUrl
          // Issue URL: https://github.com/google/ground-android/issues/2402
          BuildConfig.SIGNUP_FORM_LINK,
          onSignOut = {
            showDialog = false
            userRepository.signOut()
          },
          onCloseApp = {
            showDialog = false
            finish()
          },
        )
      }
    }
  }

  override fun onWindowInsetChanged(insets: WindowInsetsCompat) {
    super.onWindowInsetChanged(insets)
    viewModel.windowInsets.postValue(insets)
  }

  /**
   * The Android permissions API requires this callback to live in an Activity; here we dispatch the
   * result back to the PermissionManager for handling.
   */
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray,
  ) {
    Timber.d("Permission result received")
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    activityStreams.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  /**
   * The Android settings API requires this callback to live in an Activity; here we dispatch the
   * result back to the SettingsManager for handling.
   */
  @Deprecated("Deprecated in Java")
  override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
    Timber.v("Activity result received")
    super.onActivityResult(requestCode, resultCode, intent)
    activityStreams.onActivityResult(requestCode, resultCode, intent)
  }

  /** Override up button behavior to use Navigation Components back stack. */
  override fun onSupportNavigateUp(): Boolean = navigateUp()

  private fun navigateUp(): Boolean {
    val navController = navHostFragment.navController
    return if (navHostFragment.childFragmentManager.backStackEntryCount > 0) {
      navController.navigateUp()
    } else {
      false
    }
  }

  private fun dispatchBackPressed(): Boolean {
    val fragmentManager = navHostFragment.childFragmentManager
    val currentFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment)
    return currentFragment is BackPressListener && currentFragment.onBack()
  }

  private fun onSignInProgress(visible: Boolean) {
    if (visible) showSignInDialog() else dismissSignInDialog()
  }

  private fun showSignInDialog() {
    if (signInProgressDialog == null) {
      signInProgressDialog = modalSpinner(this, layoutInflater, R.string.signing_in)
    }
    signInProgressDialog?.show()
  }

  private fun dismissSignInDialog() {
    if (signInProgressDialog != null) {
      signInProgressDialog?.dismiss()
      signInProgressDialog = null
    }
  }

  private fun navigateTo(directions: NavDirections) {
    navHostFragment.navController.navigate(directions)
  }
}
