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
package com.google.android.ground.ui.datacollection

import androidx.recyclerview.widget.RecyclerView

/**
 * Abstract class extending RecyclerView.Adapter, handling the selection states of items selected in
 * the RecyclerView.
 */
abstract class SelectionAdapter<V : RecyclerView.ViewHolder> : RecyclerView.Adapter<V>() {
  abstract fun getPosition(key: Long): Int

  abstract fun handleItemStateChanged(position: Int, selected: Boolean)
}
