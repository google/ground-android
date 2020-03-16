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
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

// TODO: Add column to Observation table for storing uploaded media urls
// TODO: Synced to remote db as well
// TODO: Extract a generic interface so that we have an internal API to switch providers
@Singleton
public class FirestoreStorageManager {

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

  public Task<Uri> getDownloadUrl(String path) {
    return storageReference.child(path).getDownloadUrl();
  }

  /** Upload file to Firebase Storage. */
  public String uploadMediaFromFile(File file, String destinationPath) {
    StorageReference reference = createReference(destinationPath);
    UploadTask task = reference.putFile(Uri.fromFile(file));

    uploadMediaToFirebaseStorage(task, destinationPath);
    fetchDownloadUrl(reference, task);
    return reference.getPath();
  }

  private void uploadMediaToFirebaseStorage(UploadTask uploadTask, String fileName) {
    // TODO: Create UploadState enum and use RxJava to broadcast upload state globally.
    // use our util RxTask to convert the task to a Single.
    uploadTask
        .addOnCanceledListener(
            () -> {
              Timber.d("Uploading canceled: %s", fileName);
            })
        .addOnCompleteListener(
            task -> {
              Timber.d("Uploading completed: %s", fileName);
            })
        .addOnFailureListener(
            e -> {
              Timber.e(e, "Uploading failed: %s", fileName);
            })
        .addOnSuccessListener(
            taskSnapshot -> {
              Timber.d("Uploading succeeded: %s", fileName);
            })
        .addOnProgressListener(
            taskSnapshot -> {
              double percentCompleted =
                  100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount();
              Timber.d("Uploading in progress: %s %f", fileName, percentCompleted);
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
                Timber.d("Uploaded to : %s", downloadUri);
              }
            });
  }
}
