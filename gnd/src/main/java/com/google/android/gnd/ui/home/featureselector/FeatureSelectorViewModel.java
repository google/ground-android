package com.google.android.gnd.ui.home.featureselector;

import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.common.collect.ImmutableList;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import javax.inject.Inject;

public class FeatureSelectorViewModel extends AbstractViewModel {
  private ImmutableList<Feature> features = ImmutableList.<Feature>builder().build();
  private final PublishSubject<Integer> selections = PublishSubject.create();
  private final Observable<Feature> selectedFeatures;

  @Inject
  FeatureSelectorViewModel() {
    this.selectedFeatures = selections.map(i -> this.features.get(i));
  }

  public void onFeatures(ImmutableList<Feature> features) {
    this.features = features;
  }

  public void selectFeature(int index) {
    selections.onNext(index);
  }

  public Observable<ImmutableList<Feature>> getFeatures() {
    return Single.just(features).toObservable();
  }

  public Observable<Feature> getFeatureSelections() {
    return selectedFeatures;
  }
}
