/*
 * Copyright 2024 Google LLC
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
package com.google.android.ground.util.recyclerview

import android.content.res.Resources
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

fun atPositionOnView(recyclerViewId: Int, position: Int, targetViewId: Int): Matcher<View> =
  object : TypeSafeMatcher<View>() {
    var resources: Resources? = null
    var childView: View? = null

    override fun describeTo(description: Description) {
      var idDescription = recyclerViewId.toString()
      if (this.resources != null) {
        idDescription =
          try {
            this.resources!!.getResourceName(recyclerViewId)
          } catch (exception: Resources.NotFoundException) {
            "$recyclerViewId (resource name not found) \n ${exception.message}"
          }
      }
      description.appendText("with id: $idDescription")
    }

    public override fun matchesSafely(view: View): Boolean {
      this.resources = view.resources
      if (childView == null) {
        val recyclerView = view.rootView.findViewById<View>(recyclerViewId) as? RecyclerView
        if (recyclerView?.id == recyclerViewId) {
          childView = recyclerView.findViewHolderForAdapterPosition(position)?.itemView
        } else return false
      }
      return if (targetViewId == -1) {
        view === childView
      } else {
        view === childView?.findViewById<View>(targetViewId)
      }
    }
  }
