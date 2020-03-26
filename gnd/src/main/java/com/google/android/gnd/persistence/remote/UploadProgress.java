package com.google.android.gnd.persistence.remote;

public class UploadProgress {

  private final UploadState state;
  private final int total;
  private final int progress;

  public UploadProgress(UploadState state) {
    this(state, 0, 0);
  }

  public UploadProgress(UploadState state, int total, int progress) {
    this.state = state;
    this.total = total;
    this.progress = progress;
  }

  public int getProgress() {
    return progress;
  }

  public int getTotal() {
    return total;
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
