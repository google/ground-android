package com.google.android.gnd.ui.editobservation;

import android.net.Uri;
import android.view.View;
import androidx.databinding.ObservableMap;
import androidx.databinding.ObservableMap.OnMapChangedCallback;
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

  public void init(Field field, ObservableMap<String, Response> responses) {
    // load last saved value
    update(responses.get(field.getId()), field);

    // observe response updates
    responses.addOnMapChangedCallback(
      new OnMapChangedCallback<ObservableMap<String, Response>, String, Response>() {
        @Override
        public void onMapChanged(
          ObservableMap<String, Response> sender, String key) {
          if (key.equals(field.getId())) {
            update(sender.get(key), field);
          }
        }
      });
  }

  private void update(Response response, Field field) {
    if (response != null) {
      String value = response.getDetailsText(field);
      if (!value.isEmpty()) {
        photoPreviewVisibility.setValue(View.VISIBLE);
        disposeOnClear(
          storageManager.loadUriFromDestinationPath(value).subscribe(destinationPath::setValue)
        );
      } else {
        photoPreviewVisibility.setValue(View.GONE);
      }
    } else {
      photoPreviewVisibility.setValue(View.GONE);
    }
  }
}
