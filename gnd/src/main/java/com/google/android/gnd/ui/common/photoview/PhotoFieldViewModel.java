package com.google.android.gnd.ui.common.photoview;

import android.net.Uri;
import android.view.View;
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
  private final MutableLiveData<Integer> photoPreviewVisibility = new MutableLiveData<>(View.GONE);

  private Field field;

  @Inject
  PhotoFieldViewModel(StorageManager storageManager) {
    this.storageManager = storageManager;
  }

  public LiveData<Uri> getDestinationPath() {
    return destinationPath;
  }

  public MutableLiveData<Integer> photoPreviewVisibility() {
    return photoPreviewVisibility;
  }

  public void setField(Field field) {
    this.field = field;
  }

  public void setResponse(Response response) {
    if (field == null || response == null || response.getDetailsText(field).isEmpty()) {
      photoPreviewVisibility.postValue(View.GONE);
    } else {
      photoPreviewVisibility.postValue(View.VISIBLE);
      String path = response.getDetailsText(field);
      disposeOnClear(
          storageManager.loadUriFromDestinationPath(path).subscribe(destinationPath::postValue));
    }
  }
}
