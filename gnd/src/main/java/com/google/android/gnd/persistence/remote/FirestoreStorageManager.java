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

package com.google.android.gnd.persistence.remote;

import android.net.Uri;
import android.util.Log;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import io.reactivex.Maybe;
import java.io.File;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

// TODO: Add column to Observation table for storing uploaded media urls
// TODO: Synced to remote db as well
// TODO: Extract a generic interface so that we have an internal API to switch providers
@Singleton
public class FirestoreStorageManager {

  private static final String TAG = FirestoreStorageManager.class.getName();
  private static final String MEDIA_ROOT_DIR = "uploaded_media";
  private final StorageReference storageReference;

  @Inject
  FirestoreStorageManager(StorageReference storageReference) {
    this.storageReference = storageReference;
  }

  /** Returns a reference to the root media dir. */
  private StorageReference getRootMediaDir() {
    return storageReference.child(MEDIA_ROOT_DIR);
  }

  /**
   * Returns a reference to a object under the root media dir.
   *
   * @param fileName Name of the uploaded media
   */
  private StorageReference createReference(String fileName) {
    return getRootMediaDir().child(fileName);
  }

  public Maybe<Uri> getDownloadUrl(String path) {
    return Maybe.create(
        emitter ->
            storageReference
                .child(path)
                .getDownloadUrl()
                .addOnSuccessListener(emitter::onSuccess)
                .addOnFailureListener(emitter::onError));
  }

  /** Upload file to Firebase Storage. */
  public String uploadMediaFromFile(File file, String fileName) {
    StorageReference reference = createReference(fileName);
    UploadTask task = reference.putFile(Uri.fromFile(file));

    uploadMediaToFirebaseStorage(task, fileName);
    fetchDownloadUrl(reference, task);
    return reference.getPath();
  }

  private void uploadMediaToFirebaseStorage(UploadTask uploadTask, String fileName) {
    // TODO: Create UploadState enum and use RxJava to broadcast upload state globally.
    // use our util RxTask to convert the task to a Single.
    uploadTask
        .addOnCanceledListener(
            () -> {
              Log.d(TAG, String.format("Uploading canceled: %s", fileName));
            })
        .addOnCompleteListener(
            task -> {
              Log.d(TAG, String.format("Uploading completed: %s", fileName));
            })
        .addOnFailureListener(
            e -> {
              Log.d(TAG, String.format("Uploading failed: %s", fileName), e);
            })
        .addOnSuccessListener(
            taskSnapshot -> {
              Log.d(TAG, String.format("Uploading succeeded: %s", fileName));
            })
        .addOnProgressListener(
            taskSnapshot -> {
              double percentCompleted =
                  100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount();
              Log.d(TAG, String.format("Uploading in progress: %s %f", fileName, percentCompleted));
            });
  }

  private void fetchDownloadUrl(StorageReference reference, UploadTask uploadTask) {
    uploadTask
        .continueWithTask(
            task -> {
              if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
              }
              // Continue with the task to get the download URL
              return reference.getDownloadUrl();
            })
        .addOnCompleteListener(
            task -> {
              // TODO save to local database
              if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                Log.d(TAG, String.format("Uploaded to : %s", downloadUri));
              }
            });
  }
}
