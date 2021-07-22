package com.google.android.gnd;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

/** Lifecycle {@link Observer} helpers used for testing. */
public class TestObservers {

  /**
   * Observes the provided {@link LiveData} until the first value is emitted. This is useful for
   * testing cold LiveData streams, since by definition their upstream observables do not begin
   * emitting items until the stream is observed/subscribed to.
   */
  public static <T> void observeUntilFirstChange(LiveData<T> liveData) {
    liveData.observeForever(
        new Observer<T>() {
          @Override
          public void onChanged(T value) {
            liveData.removeObserver(this);
          }
        });
  }
}
