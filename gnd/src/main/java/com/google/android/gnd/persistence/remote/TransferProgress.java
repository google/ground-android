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

public class TransferProgress {

  private static final TransferProgress STARTING = new TransferProgress(UploadState.STARTING);
  private static final TransferProgress PAUSED = new TransferProgress(UploadState.PAUSED);
  private static final TransferProgress FAILED = new TransferProgress(UploadState.FAILED);
  private static final TransferProgress COMPLETED = new TransferProgress(UploadState.COMPLETED);

  private final UploadState state;
  private final int byteCount;
  private final int bytesTransferred;

  private TransferProgress(UploadState state) {
    this(state, 0, 0);
  }

  private TransferProgress(UploadState state, int byteCount, int bytesTransferred) {
    this.state = state;
    this.byteCount = byteCount;
    this.bytesTransferred = bytesTransferred;
  }

  public static TransferProgress starting() {
    return STARTING;
  }

  public static TransferProgress inProgress(int byteCount, int bytesTransferred) {
    return new TransferProgress(UploadState.IN_PROGRESS, byteCount, bytesTransferred);
  }

  public static TransferProgress paused() {
    return PAUSED;
  }

  public static TransferProgress failed() {
    return FAILED;
  }

  public static TransferProgress completed() {
    return COMPLETED;
  }

  public int getBytesTransferred() {
    return bytesTransferred;
  }

  public int getByteCount() {
    return byteCount;
  }

  public UploadState getState() {
    return state;
  }

  public enum UploadState {
    STARTING,
    IN_PROGRESS,
    PAUSED,
    FAILED,
    COMPLETED
  }
}
