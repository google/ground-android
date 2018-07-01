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

package com.google.android.gnd.ui.recorddetails;

import android.arch.lifecycle.LiveData;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.rx.RxLiveData;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.vo.Record;
import javax.inject.Inject;

public class RecordDetailsViewModel extends AbstractViewModel {

  private final DataRepository dataRepository;

  @Inject
  RecordDetailsViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  public LiveData<Resource<Record>> getRecordDetails(String projectId, String placeId, String recordId) {
    return RxLiveData.fromFlowable(dataRepository.getRecordDetails(projectId, placeId, recordId));
  }
}
