package com.google.android.gnd.ui.home.featureselector;

import android.text.style.TtsSpan.TimeBuilder;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.common.collect.ImmutableList;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import javax.inject.Inject;
import timber.log.Timber;

public class FeatureSelectorViewModel extends AbstractViewModel {
  private final BehaviorSubject<ImmutableList<Feature>> features = BehaviorSubject.create();
  private final PublishSubject<Integer> selections = PublishSubject.create();
  private final Observable<Feature> selectedFeatures;

  @Inject
  FeatureSelectorViewModel() {
    this.selectedFeatures = selections.flatMap(i -> features.map(features -> features.get(i)));
  }

  public void onFeatures(ImmutableList<Feature> features) {
    this.features.onNext(features);
  }

  public void selectFeature(int index) {
    selections.onNext(index);
  }

  public Observable<ImmutableList<Feature>> getFeatures() {
    return features;
  }

  public Observable<Feature> getFeatureSelections() {
    return selectedFeatures;
  }
}
