/*
 * Copyright 2023 Google LLC
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

import android.view.View
import androidx.annotation.IdRes
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.Matcher
import org.hamcrest.Matchers

/** ViewAction for item within recycler view item. */
fun <T : View> recyclerChildAction(@IdRes id: Int, block: T.() -> Unit): ViewAction =
  object : ViewAction {
    override fun getConstraints(): Matcher<View> = Matchers.any(View::class.java)

    override fun getDescription(): String = "Performing action on RecyclerView child item"

    override fun perform(uiController: UiController, view: View) {
      view.findViewById<T>(id).block()
    }
  }
