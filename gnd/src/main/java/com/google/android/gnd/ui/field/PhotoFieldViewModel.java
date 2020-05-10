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

package com.google.android.gnd.ui.field;

import static com.google.android.gnd.persistence.remote.firestore.FirestoreStorageManager.getRemoteDestinationPath;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.Config;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.system.CameraManager;
import com.google.android.gnd.system.StorageManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.editobservation.EditObservationFragmentArgs;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;
import java.io.IOException;
import java8.util.Optional;
import javax.inject.Inject;

public class PhotoFieldViewModel extends AbstractViewModel {

  private static final String EMPTY_PATH = "";
  private final StorageManager storageManager;
  private final CameraManager cameraManager;
  private final BehaviorProcessor<String> destinationPath = BehaviorProcessor.create();
  private final LiveData<Uri> uri;
  private final LiveData<Integer> visibility;
  private Field field;
  private EditObservationFragmentArgs args;

  @Inject
  PhotoFieldViewModel(StorageManager storageManager, CameraManager cameraManager) {
    this.storageManager = storageManager;
    this.cameraManager = cameraManager;
    this.visibility =
        LiveDataReactiveStreams.fromPublisher(
            destinationPath.map(path -> path.isEmpty() ? View.GONE : View.VISIBLE));
    this.uri =
        LiveDataReactiveStreams.fromPublisher(
            destinationPath.switchMapSingle(this::getDownloadUrl));
  }

  void init(Field field, EditObservationFragmentArgs args, Optional<Response> currentResponse) {
    this.field = field;
    this.args = args;

    currentResponse.ifPresent(response -> setResponse(field, response));
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

  public void setResponse(Field field, Response response) {
    if (response == null) {
      destinationPath.onNext(EMPTY_PATH);
    } else {
      destinationPath.onNext(response.getDetailsText(field));
    }
  }

  public void showPhotoSelector() {
    disposeOnClear(
        storageManager
            .launchPhotoPicker(field.getId())
            .andThen(handlePhotoPickerResult())
            .subscribe());
  }

  private Completable handlePhotoPickerResult() {
    return storageManager.photoPickerResult().flatMapCompletable(this::saveBitmapAndUpdateResponse);
  }

  public void showPhotoCapture() {
    disposeOnClear(
        cameraManager
            .launchPhotoCapture(field.getId())
            .andThen(handlePhotoCaptureResult())
            .subscribe());
  }

  private Completable handlePhotoCaptureResult() {
    return cameraManager.capturePhotoResult().flatMapCompletable(this::saveBitmapAndUpdateResponse);
  }

  private Completable saveBitmapAndUpdateResponse(Bitmap bitmap) throws IOException {
    String localFileName = field.getId() + Config.PHOTO_EXT;
    String destinationPath =
        getRemoteDestinationPath(
            args.getProjectId(), args.getFormId(), args.getFeatureId(), localFileName);
    return storageManager.savePhoto(bitmap, localFileName, destinationPath);
  }
}
