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

package com.google.android.gnd.ui.placesheet;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import com.google.android.gnd.R;

class RecordListRecyclerViewAdapter extends
  RecyclerView.Adapter<RecordListRecyclerViewAdapter.ViewHolder> {
  static class ViewHolder extends RecyclerView.ViewHolder {
//    @BindView(R.id.record_summary)
//    TextView recordSummaryTextView;

    ViewHolder(View view) {
      super(view);
      ButterKnife.bind(this, view);
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(
    @NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    View view = inflater.inflate(R.layout.record_list_item, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//    holder.recordSummaryTextView.setText("");
  }

  @Override
  public int getItemCount() {
    return 50;
  }
}
