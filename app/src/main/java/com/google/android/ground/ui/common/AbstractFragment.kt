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
package com.google.android.ground.ui.common

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.ground.AbstractActivity
import com.google.android.ground.R
import com.google.android.ground.ui.util.ViewUtil
import com.google.android.ground.util.Debug
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

abstract class AbstractFragment : Fragment() {

  @Inject lateinit var viewModelFactory: ViewModelFactory

  private var progressDialog: AlertDialog? = null

  protected fun <T : ViewModel> getViewModel(modelClass: Class<T>): T =
    viewModelFactory[this, modelClass]

  override fun onAttach(context: Context) {
    Debug.logLifecycleEvent(this)
    super.onAttach(context)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    Debug.logLifecycleEvent(this)
    super.onCreate(savedInstanceState)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    Debug.logLifecycleEvent(this)
    return super.onCreateView(inflater, container, savedInstanceState)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    Debug.logLifecycleEvent(this)
    super.onSaveInstanceState(outState)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    Debug.logLifecycleEvent(this)
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    Debug.logLifecycleEvent(this)
    super.onViewStateRestored(savedInstanceState)
  }

  override fun onStart() {
    Debug.logLifecycleEvent(this)
    super.onStart()
  }

  override fun onResume() {
    Debug.logLifecycleEvent(this)
    super.onResume()
    ViewUtil.hideSoftInputFrom(this)
  }

  override fun onPause() {
    Debug.logLifecycleEvent(this)
    super.onPause()
  }

  override fun onStop() {
    Debug.logLifecycleEvent(this)
    super.onStop()
  }

  override fun onDestroyView() {
    Debug.logLifecycleEvent(this)
    super.onDestroyView()
  }

  override fun onDestroy() {
    Debug.logLifecycleEvent(this)
    super.onDestroy()
  }

  override fun onDetach() {
    Debug.logLifecycleEvent(this)
    super.onDetach()
  }

  fun replaceFragment(@IdRes containerViewId: Int, fragment: Fragment) {
    childFragmentManager.beginTransaction().replace(containerViewId, fragment).commit()
  }

  protected fun getAbstractActivity(): AbstractActivity = requireActivity() as AbstractActivity

  protected fun showProgressDialog(@StringRes messageId: Int = R.string.loading) {
    if (progressDialog == null) {
      progressDialog = modalSpinner(messageId)
    }
    progressDialog?.show()
  }

  protected fun dismissProgressDialog() {
    if (progressDialog != null) {
      progressDialog?.dismiss()
      progressDialog = null
    }
  }

  protected fun launchWhenStarted(fn: suspend () -> Unit) {
    lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { fn() } }
  }

  protected fun <T> Flow<T>.launchWhenStartedAndCollect(collector: (T) -> Unit) {
    launchWhenStarted { this.collect { collector(it) } }
  }

  protected fun <T> Flow<T>.launchWhenStartedAndCollectFirst(collector: (T) -> Unit) {
    launchWhenStarted { collector(this.first()) }
  }
}
