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
package com.google.android.ground.ui.editsubmission

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.databinding.AddPhotoListItemBinding
import java8.util.function.Consumer

class AddPhotoDialogAdapter(private val onSelectPhotoStorageClick: Consumer<Int>) :
  RecyclerView.Adapter<AddPhotoDialogAdapter.ViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = AddPhotoListItemBinding.inflate(inflater, parent, false)
    return ViewHolder(binding, onSelectPhotoStorageClick)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val storageResource = photoStorageResources[position]
    holder.binding.textView.setText(storageResource.titleResId)
    holder.binding.imageView.setImageResource(storageResource.iconResId)
    holder.type = storageResource.sourceType
  }

  override fun getItemCount(): Int {
    return photoStorageResources.size
  }

  class ViewHolder(val binding: AddPhotoListItemBinding, consumer: Consumer<Int>) :
    RecyclerView.ViewHolder(binding.root) {
    var type = 0

    init {
      itemView.setOnClickListener { consumer.accept(type) }
    }
  }

  class PhotoStorageResource(
    @param:StringRes val titleResId: Int,
    @param:DrawableRes val iconResId: Int,
    val sourceType: Int
  ) {

    companion object {
      const val PHOTO_SOURCE_CAMERA = 1
      const val PHOTO_SOURCE_STORAGE = 2
    }
  }

  companion object {
    val photoStorageResources: List<PhotoStorageResource> =
      listOf(
        PhotoStorageResource(
          R.string.action_camera,
          R.drawable.ic_photo_camera,
          PhotoStorageResource.PHOTO_SOURCE_CAMERA
        ),
        PhotoStorageResource(
          R.string.action_storage,
          R.drawable.ic_sd_storage,
          PhotoStorageResource.PHOTO_SOURCE_STORAGE
        )
      )
  }
}
