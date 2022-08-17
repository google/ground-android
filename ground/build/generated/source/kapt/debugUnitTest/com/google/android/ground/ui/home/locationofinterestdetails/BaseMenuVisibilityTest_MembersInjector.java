package com.google.android.ground.ui.home.locationofinterestdetails;

import com.google.android.ground.test.system.auth.FakeAuthenticationManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import javax.annotation.Generated;
import javax.inject.Provider;

@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes"
})
public final class BaseMenuVisibilityTest_MembersInjector implements MembersInjector<BaseMenuVisibilityTest> {
  private final Provider<FakeAuthenticationManager> fakeAuthenticationManagerProvider;

  private final Provider<LocationOfInterestDetailsViewModel> viewModelProvider;

  public BaseMenuVisibilityTest_MembersInjector(
      Provider<FakeAuthenticationManager> fakeAuthenticationManagerProvider,
      Provider<LocationOfInterestDetailsViewModel> viewModelProvider) {
    this.fakeAuthenticationManagerProvider = fakeAuthenticationManagerProvider;
    this.viewModelProvider = viewModelProvider;
  }

  public static MembersInjector<BaseMenuVisibilityTest> create(
      Provider<FakeAuthenticationManager> fakeAuthenticationManagerProvider,
      Provider<LocationOfInterestDetailsViewModel> viewModelProvider) {
    return new BaseMenuVisibilityTest_MembersInjector(fakeAuthenticationManagerProvider, viewModelProvider);
  }

  @Override
  public void injectMembers(BaseMenuVisibilityTest instance) {
    injectFakeAuthenticationManager(instance, fakeAuthenticationManagerProvider.get());
    injectViewModel(instance, viewModelProvider.get());
  }

  @InjectedFieldSignature("com.google.android.ground.ui.home.featuredetails.BaseMenuVisibilityTest.fakeAuthenticationManager")
  public static void injectFakeAuthenticationManager(BaseMenuVisibilityTest instance,
      FakeAuthenticationManager fakeAuthenticationManager) {
    instance.fakeAuthenticationManager = fakeAuthenticationManager;
  }

  @InjectedFieldSignature("com.google.android.ground.ui.home.featuredetails.BaseMenuVisibilityTest.viewModel")
  public static void injectViewModel(BaseMenuVisibilityTest instance,
      LocationOfInterestDetailsViewModel viewModel) {
    instance.viewModel = viewModel;
  }
}
