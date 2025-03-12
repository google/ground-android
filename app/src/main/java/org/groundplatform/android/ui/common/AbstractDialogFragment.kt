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
package org.groundplatform.android.ui.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import org.groundplatform.android.util.Debug

abstract class AbstractDialogFragment : AppCompatDialogFragment() {

  @Inject lateinit var viewModelFactory: ViewModelFactory

  /** Uses [ViewModelFactory] to obtain an instance of the view model of the specified class. */
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

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    Debug.logLifecycleEvent(this)
    return super.onCreateDialog(savedInstanceState)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    Debug.logLifecycleEvent(this)
    super.onViewCreated(view, savedInstanceState)
  }

  @Deprecated("Deprecated in Java")
  override fun onAttachFragment(childFragment: Fragment) {
    Debug.logLifecycleEvent(this)
    super.onAttachFragment(childFragment)
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    Debug.logLifecycleEvent(this)
    super.onActivityCreated(savedInstanceState)
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
  }

  override fun onPause() {
    Debug.logLifecycleEvent(this)
    super.onPause()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    Debug.logLifecycleEvent(this)
    super.onSaveInstanceState(outState)
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
}
