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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject
import javax.inject.Provider

/**
 * Generic factory class to create ViewModels using Dagger 2 injections, working around the fact
 * that we cannot inject view models directly. Based on Android Architecture Components example:
 * https://github.com/googlesamples/android-architecture-components/blob/b1a194c1ae267258cd002e2e1c102df7180be473/GithubBrowserSample/app/src/main/java/com/android/example/github/viewmodel/GithubViewModelFactory.java
 */
class ViewModelFactory
@Inject
constructor(
  private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {

  /** Instantiates a new instance of the specified view model, injecting required dependencies. */
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    val creator =
      creators[modelClass]
        ?: creators.entries.firstOrNull { modelClass.isAssignableFrom(it.key) }?.value
        ?: error("Unknown model class $modelClass")
    val viewModel = creator.get()
    if (modelClass.isInstance(viewModel)) {
      @Suppress("UNCHECKED_CAST")
      return viewModel as T
    } else {
      error("Expected instance of ${modelClass.simpleName}, but got ${viewModel::class.simpleName}")
    }
  }

  /**
   * Returns an instance of the specified view model, which is scoped to the activity if annotated
   * with [SharedViewModel], or scoped to the Fragment if not.
   */
  operator fun <T : ViewModel> get(fragment: Fragment, modelClass: Class<T>): T =
    if (modelClass.getAnnotation(SharedViewModel::class.java) == null) {
      ViewModelProvider(fragment, this)[modelClass]
    } else {
      get(fragment.requireActivity(), modelClass)
    }

  /** Returns an instance of the specified view model scoped to the provided activity. */
  operator fun <T : ViewModel> get(activity: FragmentActivity, modelClass: Class<T>): T =
    ViewModelProvider(activity, this)[modelClass]
}
