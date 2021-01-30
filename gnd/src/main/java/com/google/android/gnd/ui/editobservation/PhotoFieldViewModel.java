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

import android.app.Application;
import android.net.Uri;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Field.Type;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.StorageManager;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

public class PhotoFieldViewModel extends AbstractFieldViewModel {

  private static final String EMPTY_PATH = "";

  private final StorageManager storageManager;

  @Hot(replays = true)
  private final FlowableProcessor<String> destinationPath = BehaviorProcessor.create();

  private final LiveData<Uri> uri;
  public final LiveData<Boolean> isVisible;

  @Hot(replays = true)
  private final MutableLiveData<Field> showDialogClicks = new MutableLiveData<>();

  @Hot(replays = true)
  private final MutableLiveData<Integer> clearButtonVisibility = new MutableLiveData<>(View.GONE);

  @Inject
  PhotoFieldViewModel(StorageManager storageManager, Application application) {
    super(application);
    this.storageManager = storageManager;
    this.isVisible =
        LiveDataReactiveStreams.fromPublisher(destinationPath.map(path -> !path.isEmpty()));
    this.uri =
        LiveDataReactiveStreams.fromPublisher(
            destinationPath.switchMapSingle(this::getDownloadUrl));
  }

  private Single<Uri> getDownloadUrl(String path) {
    return path.isEmpty() ? Single.just(Uri.EMPTY) : storageManager.getDownloadUrl(path);
  }

  public LiveData<Uri> getUri() {
    return uri;
  }

  @Override
  public void setResponse(Optional<Response> response) {
    super.setResponse(response);
    updateField(response.isPresent() ? response.get() : null, getField());
  }

  public void updateField(@Nullable Response response, Field field) {
    if (field.getType() != Type.PHOTO) {
      Timber.e("Not a photo type field: %s", field.getType());
      return;
    }

    if (response == null) {
      destinationPath.onNext(EMPTY_PATH);
    } else {
      destinationPath.onNext(response.getDetailsText(field));
    }
  }

  public void onShowPhotoSelectorDialog() {
    showDialogClicks.setValue(getField());
  }

  LiveData<Field> getShowDialogClicks() {
    return showDialogClicks;
  }

  public void setClearButtonVisible(boolean enabled) {
    clearButtonVisibility.postValue(enabled ? View.VISIBLE : View.GONE);
  }

  public LiveData<Integer> getClearButtonVisibility() {
    return clearButtonVisibility;
  }
}
