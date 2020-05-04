package com.google.android.gnd;

import com.google.android.gnd.inject.DaggerTestGndApplicationComponent;
import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;

public class TestGndApplication extends GndApplication {


  @Override
  protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
    // Root of dependency injection.
    return DaggerTestGndApplicationComponent.factory().create(this);
  }

}
