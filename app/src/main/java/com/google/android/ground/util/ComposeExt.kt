/*
 * Copyright 2025 Google LLC
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
package com.google.android.ground.util

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.google.android.ground.ui.theme.AppTheme

/**
 * Renders a composable function within the root view of a Fragment.
 *
 * @param composable The composable function to be rendered within the Fragment.
 * @throws IllegalStateException if the Fragment's view is null or not a [ViewGroup].
 */
fun Fragment.renderComposableDialog(composable: @Composable () -> Unit) {
  (view as ViewGroup).addView(createComposeView { composable() })
}

/**
 * Renders a composable function within the root view of an Activity.
 *
 * @param composable The composable function to be rendered within the Activity.
 * @throws IllegalStateException if the Activity's content view is null or not a [ViewGroup].
 */
fun Activity.renderComposableDialog(composable: @Composable () -> Unit) =
  findViewById<ViewGroup>(android.R.id.content).addView(createComposeView { composable() })

/**
 * Creates a [ComposeView] within a [Fragment] that hosts the provided composable function.
 *
 * @param composable The composable function to be rendered within the [ComposeView].
 * @return A [View] instance of [ComposeView] configured to display the provided composable.
 */
fun Fragment.createComposeView(composable: @Composable () -> Unit): View =
  ComposeView(requireContext()).apply { setComposableContent { composable() } }

/**
 * Creates a [ComposeView] within an [Activity] that hosts the provided composable function.
 *
 * @param composable The composable function to be rendered within the [ComposeView].
 * @return A [View] instance of [ComposeView] configured to display the provided composable.
 */
fun Activity.createComposeView(composable: @Composable () -> Unit): View =
  ComposeView(this).apply { setComposableContent { composable() } }

/**
 * Creates a [ComposeView] within a [ViewGroup] that hosts the provided composable function.
 *
 * @param composable The composable function to be rendered within the [ComposeView].
 * @return A [View] instance of [ComposeView] configured to display the provided composable.
 */
fun ViewGroup.createComposeView(composable: @Composable () -> Unit): View =
  ComposeView(context).apply { setComposableContent { composable() } }

/**
 * Sets the content of a [ComposeView] to the provided composable function wrapped in [AppTheme].
 *
 * This function configures the [ComposeView] to display the provided [composable] function,
 * ensuring that it is rendered within the application's custom [AppTheme]. This ensures that the
 * composable UI elements adhere to the application's design system.
 *
 * @param composable The composable function to be rendered within the [ComposeView].
 */
fun ComposeView.setComposableContent(composable: @Composable () -> Unit) = setContent {
  AppTheme { composable() }
}
