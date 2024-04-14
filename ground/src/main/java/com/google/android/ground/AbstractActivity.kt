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
package com.google.android.ground

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.ground.util.Debug
import com.google.android.ground.util.systemInsets
import javax.annotation.OverridingMethodsMustInvokeSuper

/** Base activity class containing common helper methods. */
abstract class AbstractActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    Debug.logLifecycleEvent(this)
    super.onCreate(savedInstanceState)
  }

  override fun setContentView(view: View) {
    super.setContentView(view)
    ViewCompat.setOnApplyWindowInsetsListener(window.decorView.rootView) { _, insets ->
      insets.apply { onWindowInsetChanged(this) }
    }
  }

  /** Adjust UI elements with respect to top/bottom insets. */
  @OverridingMethodsMustInvokeSuper
  protected open fun onWindowInsetChanged(insets: WindowInsetsCompat) {
    findViewById<View>(R.id.status_bar_scrim).layoutParams =
      FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, insets.systemInsets().top)
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

  override fun onStop() {
    Debug.logLifecycleEvent(this)
    super.onStop()
  }

  override fun onDestroy() {
    Debug.logLifecycleEvent(this)
    super.onDestroy()
  }
}
