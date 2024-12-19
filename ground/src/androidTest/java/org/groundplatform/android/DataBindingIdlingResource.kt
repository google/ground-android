/*
 * Copyright 2020 Google LLC
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
package org.groundplatform.android

import android.R
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingResource
import java.util.*

/**
 * Used to make Espresso work with DataBinding. Without it the tests will be flaky because
 * DataBinding uses Choreographer class to synchronize its view updates hence using this to monitor
 * a launched fragment in fragment scenario will make Espresso wait before doing additional checks
 */
class DataBindingIdlingResource : IdlingResource {
  // Holds whether isIdle was called and the result was false. We track this to avoid calling
  // onTransitionToIdle callbacks if Espresso never thought we were idle in the first place.
  private var wasNotIdle = false
  private var activity: FragmentActivity? = null

  override fun getName(): String = String.format(Locale.getDefault(), "DataBinding $ID")

  override fun isIdleNow(): Boolean {
    val idle = bindings().none { it?.hasPendingBindings() ?: false }
    if (idle) {
      if (wasNotIdle) {
        // Notify observers to avoid Espresso race detector.
        for (cb in IDLING_CALLBACKS) {
          cb.onTransitionToIdle()
        }
      }
      wasNotIdle = false
    } else {
      wasNotIdle = true
      // Check next frame.
      if (activity != null) {
        activity!!.findViewById<View>(R.id.content).postDelayed({ this.isIdleNow }, 16)
      }
    }
    return idle
  }

  override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
    IDLING_CALLBACKS.add(callback)
  }

  /** Sets the activity from an [ActivityScenario] to be used from [DataBindingIdlingResource]. */
  fun <T : FragmentActivity?> monitorActivity(activityScenario: ActivityScenario<T>) {
    activityScenario.onActivity { activity: T -> this.activity = activity }
  }

  private fun getBinding(view: View): ViewDataBinding? = DataBindingUtil.getBinding(view)

  private fun bindings(): List<ViewDataBinding?> {
    val fragments = activity?.supportFragmentManager?.fragments ?: emptyList()
    val bindings: MutableList<ViewDataBinding?> = ArrayList()
    for (f in fragments) {
      val fv: View = f.view ?: continue
      bindings.add(getBinding(fv))
      for (cf in f.childFragmentManager.fragments) {
        val cfv: View = cf.view ?: continue
        bindings.add(getBinding(cfv))
        for (cf2 in cf.childFragmentManager.fragments) {
          val cf2v: View = cf2.view ?: continue
          bindings.add(getBinding(cf2v))
        }
      }
    }
    return bindings
  }

  companion object {
    // Give it a unique id to work around an Espresso bug where you cannot register/unregister
    // an idling resource with the same name.
    private val ID = UUID.randomUUID().toString()

    // List of registered callbacks
    private val IDLING_CALLBACKS: MutableList<IdlingResource.ResourceCallback> = ArrayList()
  }
}
