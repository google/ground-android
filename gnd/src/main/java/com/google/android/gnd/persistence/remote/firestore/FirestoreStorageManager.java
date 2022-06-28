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

// TODO: Add column to Submission table for storing uploaded media urls
// TODO: Synced to remote db as well
@Singleton
public class FirestoreStorageManager implements RemoteStorageManager {

  /**
   * Top-level directory in Cloud Storage where user media is stored.
   */
  private static final String MEDIA_ROOT_DIR = "user-media";

  @Inject
  StorageReference storageReference;

  @Inject
  FirestoreStorageManager() {
  }

  /**
   * Generates destination path in which an submission attachment is to be stored in to Cloud
   * Storage.
   *
   * <p>user-media/surveys/{survey_id}/submissions/{submission_id}/{field_id-uuid.jpg}
   */
  public static String getRemoteMediaPath(String surveyId, String submissionId, String filename) {
    // TODO: Refactor this into MediaStorageRepository.
    return new StringJoiner(File.separator)
        .add(MEDIA_ROOT_DIR)
        .add("surveys")
        .add(surveyId)
        .add("submissions")
        .add(submissionId)
        .add(filename)
        .toString();
  }

  private StorageReference createReference(String path) {
    return storageReference.child(path);
  }

  @Cold
  @Override
  public Single<Uri> getDownloadUrl(String remoteDestinationPath) {
    // StorageException's constructor logs errors, so even though we handle the exception,
    // an ERROR level log line is added which could be misleading to developers. We log an extra
    // error message here as an extra hint that the log line is probably noise.
    return RxTask.toSingle(() -> createReference(remoteDestinationPath).getDownloadUrl())
        .doOnError(e -> Timber.e("StorageException handled and can be ignored"));
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
                      // Do not delete the file after successful upload. It is used as a cache
                      // while viewing submissions when network is unavailable.
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
