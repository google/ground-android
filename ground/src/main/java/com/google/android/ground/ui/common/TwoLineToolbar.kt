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
package com.google.android.ground.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.Toolbar
import com.google.android.ground.databinding.TwoLineToolbarBinding

class TwoLineToolbar(context: Context, attrs: AttributeSet?) : Toolbar(context, attrs) {

  private val binding: TwoLineToolbarBinding =
    TwoLineToolbarBinding.inflate(LayoutInflater.from(getContext()), this, true)

  fun setTitle(title: String?) {
    binding.toolbarTitleText.text = title
  }

  fun setSubtitle(subtitle: String?) {
    binding.toolbarSubtitleText.text = subtitle
    binding.toolbarSubtitleText.visibility = if (subtitle.isNullOrEmpty()) GONE else VISIBLE
  }
}
