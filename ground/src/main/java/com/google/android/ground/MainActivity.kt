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
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.android.ground.databinding.MainActBinding
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.ActivityCallback
import com.google.android.ground.system.ActivityStreams
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.common.BackPressListener
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.FinishApp
import com.google.android.ground.ui.common.NavigateTo
import com.google.android.ground.ui.common.NavigateUp
import com.google.android.ground.ui.common.NavigationRequest
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.common.ProgressDialogs.modalSpinner
import com.google.android.ground.ui.common.ViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The app's main activity. The app consists of multiples Fragments that live under this activity.
 */
@AndroidEntryPoint
class MainActivity : AbstractActivity() {
  @Inject lateinit var activityStreams: ActivityStreams
  @Inject lateinit var viewModelFactory: ViewModelFactory
  @Inject lateinit var settingsManager: SettingsManager
  @Inject lateinit var navigator: Navigator
  @Inject lateinit var userRepository: UserRepository
  @Inject lateinit var popups: EphemeralPopups

  private lateinit var viewModel: MainViewModel
  private lateinit var navHostFragment: NavHostFragment

  private var signInProgressDialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    // Make sure this is before calling super.onCreate()
    setTheme(R.style.AppTheme)
    super.onCreate(savedInstanceState)

    // Set up event streams first. Navigator must be listening when auth is first initialized.
    lifecycleScope.launch {
      activityStreams.activityRequests.collect { callback: ActivityCallback ->
        callback(this@MainActivity)
      }
    }

    lifecycleScope.launch { navigator.getNavigateRequests().collect { onNavigate(it) } }

    val binding = MainActBinding.inflate(layoutInflater)
    setContentView(binding.root)

    navHostFragment =
      supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

    viewModel = viewModelFactory.get(this, MainViewModel::class.java)
    viewModel.signInProgressDialogVisibility.observe(this) { visible: Boolean ->
      onSignInProgress(visible)
    }
  }

  override fun onWindowInsetChanged(insets: WindowInsetsCompat) {
    super.onWindowInsetChanged(insets)
    viewModel.windowInsets.postValue(insets)
  }

  private fun onNavigate(navRequest: NavigationRequest) {
    when (navRequest) {
      is NavigateTo -> navHostFragment.navController.navigate(navRequest.directions)
      is NavigateUp -> navigateUp()
      is FinishApp -> finish()
    }
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
    Timber.d("Activity result received")
    super.onActivityResult(requestCode, resultCode, intent)
    activityStreams.onActivityResult(requestCode, resultCode, intent)
  }

  /** Override up button behavior to use Navigation Components back stack. */
  override fun onSupportNavigateUp(): Boolean = navigateUp()

  private fun navigateUp(): Boolean {
    val navController = navHostFragment.navController
    return navController.navigateUp()
  }

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    if (!dispatchBackPressed()) super.onBackPressed()
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
}
