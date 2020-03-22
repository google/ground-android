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
import com.google.android.gms.tasks.Task;
import com.google.android.gnd.persistence.remote.RemoteStorageManager;
import com.google.firebase.storage.StorageReference;
import io.reactivex.Completable;
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
  public static String getRemoteImagePath(
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

  @Override
  public Task<Uri> getDownloadUrl(String remoteDestinationPath) {
    return createReference(remoteDestinationPath).getDownloadUrl();
  }

  /** Upload file to Firebase Storage. */
  @Override
  public Completable uploadMediaFromFile(File file, String remoteDestinationPath) {
    return Completable.create(
        emitter ->
            createReference(remoteDestinationPath)
                .putFile(Uri.fromFile(file))
                .addOnCompleteListener(uploadTask -> emitter.onComplete())
                .addOnFailureListener(emitter::onError)
                .addOnSuccessListener(
                    taskSnapshot -> {
                      // TODO: taskSnapshot contains metadata that can be displayed after uploading
                    })
                .addOnProgressListener(
                    taskSnapshot -> {
                      // TODO: Display upload status in app or notification
                      double completed =
                          100.0
                              * taskSnapshot.getBytesTransferred()
                              / taskSnapshot.getTotalByteCount();
                      Timber.d("Uploading in progress: %s %f", remoteDestinationPath, completed);
                    }));
  }
}
