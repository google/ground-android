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

public class UploadProgress {

  private static final UploadProgress STARTING = new UploadProgress(UploadState.STARTING);
  private static final UploadProgress PAUSED = new UploadProgress(UploadState.PAUSED);
  private static final UploadProgress FAILED = new UploadProgress(UploadState.FAILED);
  private static final UploadProgress COMPLETED = new UploadProgress(UploadState.COMPLETED);

  private final UploadState state;
  private final int byteCount;
  private final int byteTransferred;

  private UploadProgress(UploadState state) {
    this(state, 0, 0);
  }

  private UploadProgress(UploadState state, int byteCount, int byteTransferred) {
    this.state = state;
    this.byteCount = byteCount;
    this.byteTransferred = byteTransferred;
  }

  public static UploadProgress starting() {
    return STARTING;
  }

  public static UploadProgress inProgress(int byteCount, int byteTransferred) {
    return new UploadProgress(UploadState.IN_PROGRESS, byteCount, byteTransferred);
  }

  public static UploadProgress paused() {
    return PAUSED;
  }

  public static UploadProgress failed() {
    return FAILED;
  }

  public static UploadProgress completed() {
    return COMPLETED;
  }

  public int getByteTransferred() {
    return byteTransferred;
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
