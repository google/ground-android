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

import static com.google.android.gnd.persistence.remote.firestore.FirestoreStorageManager.getRemoteMediaPath;

import android.content.res.Resources;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.android.gnd.repository.UserMediaRepository;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.editobservation.EditObservationViewModel.PhotoResult;
import java.io.File;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.inject.Inject;
import timber.log.Timber;

public class PhotoFieldViewModel extends AbstractFieldViewModel {

  private final UserMediaRepository userMediaRepository;

  private final LiveData<Uri> uri;
  private final LiveData<Boolean> photoPresent;

  @Nullable private String projectId;
  @Nullable private String observationId;

  @Hot(replays = true)
  private final MutableLiveData<Field> showDialogClicks = new MutableLiveData<>();

  @Hot(replays = true)
  private final MutableLiveData<Boolean> editable = new MutableLiveData<>(false);

  @Inject
  PhotoFieldViewModel(UserMediaRepository userMediaRepository, Resources resources) {
    super(resources);
    this.userMediaRepository = userMediaRepository;
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

  public void setProjectId(@Nullable String projectId) {
    this.projectId = projectId;
  }

  public void setObservationId(@Nullable String observationId) {
    this.observationId = observationId;
  }

  public void onPhotoResult(PhotoResult photoResult) {
    if (photoResult.isHandled()) {
      return;
    }
    if (projectId == null || observationId == null) {
      Timber.e("projectId or observationId not set");
      return;
    }
    if (!photoResult.hasFieldId(getField().getId())) {
      // Update belongs to another field.
      return;
    }
    photoResult.setHandled(true);
    if (photoResult.isEmpty()) {
      clearResponse();
      Timber.v("Photo cleared");
      return;
    }
    try {
      File imageFile = getFileFromResult(photoResult);
      String filename = imageFile.getName();
      String path = imageFile.getAbsolutePath();

      // Add image to gallery.
      userMediaRepository.addImageToGallery(path, filename);

      // Update response.
      String remoteDestinationPath = getRemoteMediaPath(projectId, observationId, filename);
      updateResponse(remoteDestinationPath);
    } catch (IOException e) {
      // TODO: Report error.
      Timber.e(e, "Failed to save photo");
    }
  }

  private File getFileFromResult(PhotoResult result) throws IOException {
    if (result.getBitmap().isPresent()) {
      return userMediaRepository.savePhoto(result.getBitmap().get(), result.getFieldId());
    }
    if (result.getPath().isPresent()) {
      return new File(result.getPath().get());
    }
    throw new IllegalStateException("PhotoResult is empty");
  }
}
