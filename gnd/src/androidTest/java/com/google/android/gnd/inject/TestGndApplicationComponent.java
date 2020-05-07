package com.google.android.gnd.inject;

import com.google.android.gnd.GndApplication;
import com.google.android.gnd.GndApplicationModule;
import com.google.android.gnd.TestMainActivityInjectorModule;
import com.google.android.gnd.persistence.local.LocalDataStoreModule;
import com.google.android.gnd.ui.map.MapProviderModule;
import dagger.Component;
import dagger.android.AndroidInjector;
import javax.inject.Singleton;

@Singleton
@Component(
  modules = {
    GndApplicationModule.class,
    MapProviderModule.class,
    LocalDataStoreModule.class,
    TestMainActivityInjectorModule.class
    })
public interface TestGndApplicationComponent extends AndroidInjector<GndApplication> {

  /**
   * Here we have a factory. We are using AndroidInjector.Factory to implement it.
   * dagger-android removes the boilerplate of passing in the context
   */
  @Component.Factory
  interface Factory extends AndroidInjector.Factory<GndApplication> {}
}
