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
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * This function may be called on a [RecyclerView] resource ID for matching [text] at a given
 * [position].
 */
fun Int.shouldHaveTextAtPosition(text: String, position: Int) {
  Espresso.onView(ViewMatchers.withId(this))
    .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position))
    .check(matches(atPosition(position, hasDescendant(ViewMatchers.withText(text)))))
}

private fun atPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<in View> {
  return object : BoundedMatcher<View?, RecyclerView?>(RecyclerView::class.java) {
    override fun describeTo(description: Description) {
      description.appendText("has item at position $position: ")
      itemMatcher.describeTo(description)
    }

    override fun matchesSafely(view: RecyclerView?): Boolean {
      val viewHolder = view?.findViewHolderForAdapterPosition(position) ?: return false
      return itemMatcher.matches(viewHolder.itemView)
    }
  }
}
