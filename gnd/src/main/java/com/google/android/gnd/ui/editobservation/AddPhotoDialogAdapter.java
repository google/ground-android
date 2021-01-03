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

package com.google.android.gnd.ui.editobservation;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.AddPhotoListItemBinding;
import com.google.common.collect.ImmutableList;
import java8.util.function.Consumer;

public class AddPhotoDialogAdapter extends RecyclerView.Adapter<AddPhotoDialogAdapter.ViewHolder> {

  private static final ImmutableList<PhotoStorageResource> PHOTO_STORAGE_RESOURCES =
      ImmutableList.of(
          new PhotoStorageResource(
              R.string.action_camera,
              R.drawable.ic_photo_camera,
              PhotoStorageResource.PHOTO_SOURCE_CAMERA),
          new PhotoStorageResource(
              R.string.action_storage,
              R.drawable.ic_sd_storage,
              PhotoStorageResource.PHOTO_SOURCE_STORAGE));

  private final Consumer<Integer> onSelectPhotoStorageClick;

  public AddPhotoDialogAdapter(Consumer<Integer> onSelectPhotoStorageClick) {
    this.onSelectPhotoStorageClick = onSelectPhotoStorageClick;
  }

  public static ImmutableList<PhotoStorageResource> getPhotoStorageResources() {
    return PHOTO_STORAGE_RESOURCES;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    AddPhotoListItemBinding binding = AddPhotoListItemBinding.inflate(inflater, parent, false);
    return new ViewHolder(binding, onSelectPhotoStorageClick);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    PhotoStorageResource storageResource = getPhotoStorageResources().get(position);
    holder.binding.textView.setText(storageResource.getTitleResId());
    holder.binding.imageView.setImageResource(storageResource.getIconResId());
    holder.type = storageResource.getSourceType();
  }

  @Override
  public int getItemCount() {
    return getPhotoStorageResources().size();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final AddPhotoListItemBinding binding;
    private int type;

    public ViewHolder(@NonNull AddPhotoListItemBinding binding, Consumer<Integer> consumer) {
      super(binding.getRoot());
      this.binding = binding;
      this.itemView.setOnClickListener(__ -> consumer.accept(type));
    }
  }

  public static class PhotoStorageResource {

    public static final int PHOTO_SOURCE_CAMERA = 1;
    public static final int PHOTO_SOURCE_STORAGE = 2;

    private final int titleResId;
    private final int iconResId;
    private final int sourceType;

    public PhotoStorageResource(
        @StringRes int titleResId, @DrawableRes int iconResId, int sourceType) {
      this.titleResId = titleResId;
      this.iconResId = iconResId;
      this.sourceType = sourceType;
    }

    public int getIconResId() {
      return iconResId;
    }

    public int getSourceType() {
      return sourceType;
    }

    public int getTitleResId() {
      return titleResId;
    }
  }
}
