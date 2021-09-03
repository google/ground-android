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

import android.content.res.Resources;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.android.gnd.repository.UserMediaRepository;
import com.google.android.gnd.rx.annotations.Hot;
import javax.inject.Inject;

public class PhotoFieldViewModel extends AbstractFieldViewModel {

  private final LiveData<Uri> uri;
  private final LiveData<Boolean> photoPresent;

  @Hot(replays = true)
  private final MutableLiveData<Field> showDialogClicks = new MutableLiveData<>();

  @Hot(replays = true)
  private final MutableLiveData<Boolean> editable = new MutableLiveData<>(false);

  @Inject
  PhotoFieldViewModel(UserMediaRepository userMediaRepository, Resources resources) {
    super(resources);
    this.photoPresent =
        LiveDataReactiveStreams.fromPublisher(
            getDetailsTextFlowable().map(path -> !path.isEmpty()));
    this.uri =
        LiveDataReactiveStreams.fromPublisher(
            getDetailsTextFlowable().switchMapSingle(userMediaRepository::getDownloadUrl));
  }

  public LiveData<Uri> getUri() {
    return uri;
  }

  public void onShowPhotoSelectorDialog() {
    showDialogClicks.setValue(getField());
  }

  LiveData<Field> getShowDialogClicks() {
    return showDialogClicks;
  }

  public void setEditable(boolean enabled) {
    editable.postValue(enabled);
  }

  public LiveData<Boolean> isPhotoPresent() {
    return photoPresent;
  }

  public LiveData<Boolean> isEditable() {
    return editable;
  }

  public void updateResponse(String value) {
    setResponse(TextResponse.fromString(value));
  }
}
