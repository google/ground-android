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

package com.google.android.gnd.persistence.remote.firestore;

import android.net.Uri;
import com.google.android.gnd.persistence.remote.RemoteStorageManager;
import com.google.android.gnd.persistence.remote.TransferProgress;
import com.google.android.gnd.rx.RxTask;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.firebase.storage.StorageReference;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.io.File;
import java8.util.StringJoiner;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

// TODO: Add column to Observation table for storing uploaded media urls
// TODO: Synced to remote db as well
@Singleton
public class FirestoreStorageManager implements RemoteStorageManager {

  // Top-level directory
  private static final String MEDIA_ROOT_DIR = "uploaded_media";

  @Inject StorageReference storageReference;

  @Inject
  FirestoreStorageManager() {}

  /**
   * Generates destination path for saving the image to Firestore Storage.
   *
   * <p>/uploaded_media/{project_id}/{form_id}/{feature_id}/{filename.jpg}
   */
  public static String getRemoteDestinationPath(
      String projectId, String formId, String featureId, String filename) {
    return new StringJoiner(File.separator)
        .add(MEDIA_ROOT_DIR)
        .add(projectId)
        .add(formId)
        .add(featureId)
        .add(filename)
        .toString();
  }

  private StorageReference createReference(String path) {
    return storageReference.child(path);
  }

  @Cold
  @Override
  public Single<Uri> getDownloadUrl(String remoteDestinationPath) {
    return RxTask.toSingle(() -> createReference(remoteDestinationPath).getDownloadUrl());
  }

  @Cold
  @Override
  public Flowable<TransferProgress> uploadMediaFromFile(File file, String remoteDestinationPath) {
    return Flowable.create(
        emitter ->
            createReference(remoteDestinationPath)
                .putFile(Uri.fromFile(file))
                .addOnCompleteListener(
                    uploadTask -> {
                      if (file.delete()) {
                        Timber.d("File deleted: %s", file.getName());
                      }
                      emitter.onComplete();
                    })
                .addOnPausedListener(taskSnapshot -> emitter.onNext(TransferProgress.paused()))
                .addOnFailureListener(emitter::onError)
                .addOnProgressListener(
                    taskSnapshot ->
                        emitter.onNext(
                            TransferProgress.inProgress(
                                (int) taskSnapshot.getTotalByteCount(),
                                (int) taskSnapshot.getBytesTransferred()))),
        BackpressureStrategy.LATEST);
  }
}
