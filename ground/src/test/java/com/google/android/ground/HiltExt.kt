/*
 * Copyright 2022 Google LLC
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

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.core.util.Preconditions
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider

/**
 * `launchFragmentInContainer` from the androidx.fragment:fragment-testing library is NOT possible
 * to use right now as it uses a hardcoded Activity under the hood (i.e. `EmptyFragmentActivity`)
 * which is not annotated with @AndroidEntryPoint. As a workaround, use this function that is
 * equivalent. It requires you to add [HiltTestActivity] in the debug folder and include it in the
 * debug AndroidManifest.xml file as can be found in this project.
 */
inline fun <reified T : Fragment> launchFragmentInHiltContainer(
  fragmentArgs: Bundle? = null,
  @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
  crossinline action: Fragment.() -> Unit = {},
): ActivityScenario<HiltTestActivity> =
  hiltActivityScenario(themeResId).launchFragment<T>(fragmentArgs, {}) { this.action() }

/** Instantiates a new activity scenario with Hilt support. */
fun hiltActivityScenario(
  @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
): ActivityScenario<HiltTestActivity> {
  val startActivityIntent =
    Intent.makeMainActivity(
        ComponentName(ApplicationProvider.getApplicationContext(), HiltTestActivity::class.java)
      )
      .putExtra(
        "androidx.fragment.app.testing.FragmentScenario.EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY",
        themeResId
      )

  return ActivityScenario.launch<HiltTestActivity>(startActivityIntent)
}

/**
 * Launch a fragment in a Hilt enabled ActivityScenario. If needed, provide fragment arguments and
 * fragment actions to run prior to and after the fragment transaction is committed.
 */
inline fun <reified T : Fragment> ActivityScenario<HiltTestActivity>.launchFragment(
  fragmentArgs: Bundle? = null,
  crossinline preTransactionAction: Fragment.() -> Unit = {},
  crossinline postTransactionAction: Fragment.() -> Unit = {}
): ActivityScenario<HiltTestActivity> =
  this.onActivity { activity ->
    val fragment: Fragment =
      activity.supportFragmentManager.fragmentFactory.instantiate(
        Preconditions.checkNotNull(T::class.java.classLoader),
        T::class.java.name
      )
    fragment.arguments = fragmentArgs

    fragment.preTransactionAction()

    activity.supportFragmentManager
      .beginTransaction()
      .add(android.R.id.content, fragment, "")
      .commitNow()

    fragment.postTransactionAction()
  }
