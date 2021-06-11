package com.google.android.gnd.ui.tos;

import com.google.android.gnd.repository.TermsOfServiceRepository;
import com.google.android.gnd.ui.common.Navigator;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes"
})
public final class TermsOfServiceViewModel_Factory implements Factory<TermsOfServiceViewModel> {
  private final Provider<Navigator> navigatorProvider;

  private final Provider<TermsOfServiceRepository> termsOfServiceRepositoryProvider;

  public TermsOfServiceViewModel_Factory(Provider<Navigator> navigatorProvider,
      Provider<TermsOfServiceRepository> termsOfServiceRepositoryProvider) {
    this.navigatorProvider = navigatorProvider;
    this.termsOfServiceRepositoryProvider = termsOfServiceRepositoryProvider;
  }

  @Override
  public TermsOfServiceViewModel get() {
    return newInstance(navigatorProvider.get(), termsOfServiceRepositoryProvider.get());
  }

  public static TermsOfServiceViewModel_Factory create(Provider<Navigator> navigatorProvider,
      Provider<TermsOfServiceRepository> termsOfServiceRepositoryProvider) {
    return new TermsOfServiceViewModel_Factory(navigatorProvider, termsOfServiceRepositoryProvider);
  }

  public static TermsOfServiceViewModel newInstance(Navigator navigator,
      TermsOfServiceRepository termsOfServiceRepository) {
    return new TermsOfServiceViewModel(navigator, termsOfServiceRepository);
  }
}
