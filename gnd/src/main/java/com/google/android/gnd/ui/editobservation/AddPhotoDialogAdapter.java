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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.AddPhotoListItemBinding;

public class AddPhotoDialogAdapter extends RecyclerView.Adapter<AddPhotoDialogAdapter.ViewHolder> {

  public static final int CAMERA = 1;
  public static final int STORAGE = 2;

  private static final int[][] ADD_PHOTO_LISt = {
    {R.string.action_camera, R.drawable.ic_photo_camera, CAMERA},
    {R.string.action_storage, R.drawable.ic_sd_storage, STORAGE}
  };

  private ItemClickListener listener;

  public AddPhotoDialogAdapter(ItemClickListener listener) {
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    AddPhotoListItemBinding binding =
        AddPhotoListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding, listener);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.binding.textView.setText(ADD_PHOTO_LISt[position][0]);
    holder.binding.imageView.setImageResource(ADD_PHOTO_LISt[position][1]);
    holder.type = ADD_PHOTO_LISt[position][2];
  }

  @Override
  public int getItemCount() {
    return ADD_PHOTO_LISt.length;
  }

  public interface ItemClickListener {
    void onClick(int type);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder implements OnClickListener {

    private final AddPhotoListItemBinding binding;
    private final ItemClickListener listener;
    private int type;

    public ViewHolder(@NonNull AddPhotoListItemBinding binding, ItemClickListener listener) {
      super(binding.getRoot());
      this.binding = binding;
      this.listener = listener;
      itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
      listener.onClick(type);
    }
  }
}
