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

package com.google.android.gnd.ui.home.featuresheet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;
import com.google.android.gnd.vo.Record;
import java.util.Collections;
import java.util.List;

// TODO: Consider passing in ViewModel and using DataBinding like todoapp example.
class RecordListAdapter extends RecyclerView.Adapter<RecordListItemViewHolder> {

  private List<Record> recordSummaries;
  private MutableLiveData<Record> itemClicks;

  public RecordListAdapter() {
    recordSummaries = Collections.emptyList();
    itemClicks = new MutableLiveData<>();
  }

  @NonNull
  @Override
  public RecordListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return RecordListItemViewHolder.newInstance(parent, itemClicks);
  }

  @Override
  public void onBindViewHolder(@NonNull RecordListItemViewHolder holder, int position) {
    holder.update(recordSummaries.get(position));
  }

  @Override
  public int getItemCount() {
    return recordSummaries.size();
  }

  LiveData<Record> getItemClicks() {
    return itemClicks;
  }

  void update(List<Record> recordSummaries) {
    this.recordSummaries = recordSummaries;
    notifyDataSetChanged();
  }
}
