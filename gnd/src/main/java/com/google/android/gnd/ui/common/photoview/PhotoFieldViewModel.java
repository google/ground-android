package com.google.android.gnd.ui.common.photoview;

import android.net.Uri;
import android.view.View;
import androidx.databinding.ObservableInt;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.system.StorageManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import javax.inject.Inject;

public class PhotoFieldViewModel extends AbstractViewModel {

  private final StorageManager storageManager;
  private final MutableLiveData<Uri> destinationPath = new MutableLiveData<Uri>();
  private final ObservableInt isDestinationPathAvailable = new ObservableInt(View.GONE);

  private Field field;

  @Inject
  PhotoFieldViewModel(StorageManager storageManager) {
    this.storageManager = storageManager;
  }

  public LiveData<Uri> getDestinationPath() {
    return destinationPath;
  }

  public ObservableInt isDestinationPathAvailable() {
    return isDestinationPathAvailable;
  }

  public void setField(Field field) {
    this.field = field;
  }

  public void setResponse(Response response) {
    if (field == null || response == null || response.getDetailsText(field).isEmpty()) {
      isDestinationPathAvailable.set(View.GONE);
    } else {
      isDestinationPathAvailable.set(View.VISIBLE);
      String path = response.getDetailsText(field);
      disposeOnClear(
          storageManager.loadUriFromDestinationPath(path).subscribe(destinationPath::postValue));
    }
  }
}
