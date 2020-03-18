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

import android.net.Uri;
import android.view.View;
import androidx.databinding.ObservableMap;
import androidx.databinding.ObservableMap.OnMapChangedCallback;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.system.StorageManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import javax.inject.Inject;

public class PhotoFieldViewModel extends AbstractViewModel {

  private static final String EMPTY_PATH = "";
  private final StorageManager storageManager;
  private final BehaviorProcessor<String> destinationPath = BehaviorProcessor.create();
  private final LiveData<Uri> uri;
  private final LiveData<Integer> visibility;

  @Inject
  PhotoFieldViewModel(StorageManager storageManager) {
    this.storageManager = storageManager;
    this.visibility =
        LiveDataReactiveStreams.fromPublisher(
            destinationPath.map(path -> path.isEmpty() ? View.GONE : View.VISIBLE));
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

  public LiveData<Integer> getVisibility() {
    return visibility;
  }

  void init(Field field, ObservableMap<String, Response> responses) {
    // Load last saved value
    updateField(responses.get(field.getId()), field);

    // Observe response updates
    responses.addOnMapChangedCallback(
        new OnMapChangedCallback<ObservableMap<String, Response>, String, Response>() {
          @Override
          public void onMapChanged(ObservableMap<String, Response> sender, String key) {
            if (key.equals(field.getId())) {
              updateField(sender.get(key), field);
            }
          }
        });
  }

  private void updateField(Response response, Field field) {
    if (response != null) {
      destinationPath.onNext(response.getDetailsText(field));
    } else {
      destinationPath.onNext(EMPTY_PATH);
    }
  }
}
