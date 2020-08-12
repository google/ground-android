package com.google.android.gnd.persistence.remote;

import android.net.Uri;
import com.google.android.gms.tasks.Task;
import io.reactivex.Flowable;
import java.io.File;
import javax.inject.Inject;

public class FakeRemoteStorageManager implements RemoteStorageManager {

  @Inject
  FakeRemoteStorageManager(){}

  @Override
  public Task<Uri> getDownloadUrl(
    String remoteDestinationPath) {
    return null;
  }

  @Override
  public Flowable<TransferProgress> uploadMediaFromFile(File file,
    String remoteDestinationPath) {
    return null;
  }
}
