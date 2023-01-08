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

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.google.android.ground.databinding.MainActBinding
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.rx.RxAutoDispose.autoDisposable
import com.google.android.ground.system.ActivityStreams
import com.google.android.ground.system.ApplicationErrorManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.common.*
import dagger.hilt.android.AndroidEntryPoint
import java8.util.function.Consumer
import javax.inject.Inject
import timber.log.Timber

/**
 * The app's main activity. The app consists of multiples Fragments that live under this activity.
 */
@AndroidEntryPoint
class MainActivity : AbstractActivity() {
  @Inject lateinit var activityStreams: ActivityStreams

  @Inject lateinit var errorManager: ApplicationErrorManager

  @Inject lateinit var viewModelFactory: ViewModelFactory

  @Inject lateinit var settingsManager: SettingsManager

  @Inject lateinit var navigator: Navigator

  @Inject lateinit var userRepository: UserRepository

  @Inject lateinit var popups: EphemeralPopups

  private lateinit var viewModel: MainViewModel
  private lateinit var navHostFragment: NavHostFragment

  private var signInProgressDialog: ProgressDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    // Make sure this is before calling super.onCreate()
    setTheme(R.style.AppTheme)
    super.onCreate(savedInstanceState)

    // Set up event streams first. Navigator must be listening when auth is first initialized.
    activityStreams.activityRequests.`as`(autoDisposable(this)).subscribe {
      callback: Consumer<Activity> ->
      callback.accept(this)
    }

    navigator.getNavigateRequests().`as`(autoDisposable(this)).subscribe {
      navDirections: NavDirections ->
      onNavigate(navDirections)
    }

    navigator.getNavigateUpRequests().`as`(autoDisposable(this)).subscribe { navigateUp() }

    val binding = MainActBinding.inflate(layoutInflater)
    setContentView(binding.root)

    navHostFragment =
      supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

    viewModel = viewModelFactory.get(this, MainViewModel::class.java)
    viewModel.signInProgressDialogVisibility.observe(this) { visible: Boolean ->
      onSignInProgress(visible)
    }

    errorManager.exceptions.`as`(autoDisposable(this)).subscribe { onUnrecoverableError(it) }
  }

  private fun onUnrecoverableError(message: String) {
    popups.showError(message)
    finish()
  }

  override fun onWindowInsetChanged(insets: WindowInsetsCompat) {
    super.onWindowInsetChanged(insets)
    viewModel.windowInsets.postValue(insets)
  }

  private fun onNavigate(navDirections: NavDirections) {
    val navController = navHostFragment.navController
    navController.navigate(navDirections)
  }

  /**
   * The Android permissions API requires this callback to live in an Activity; here we dispatch the
   * result back to the PermissionManager for handling.
   */
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    Timber.d("Permission result received")
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    activityStreams.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  /**
   * The Android settings API requires this callback to live in an Activity; here we dispatch the
   * result back to the SettingsManager for handling.
   */
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

  override fun onToolbarUpClicked() {
    if (!dispatchBackPressed()) navigateUp()
  }

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
      signInProgressDialog = ProgressDialogs.modalSpinner(this, R.string.signing_in)
    }
    signInProgressDialog!!.show()
  }

  private fun dismissSignInDialog() {
    if (signInProgressDialog != null) {
      signInProgressDialog!!.dismiss()
      signInProgressDialog = null
    }
  }
}
