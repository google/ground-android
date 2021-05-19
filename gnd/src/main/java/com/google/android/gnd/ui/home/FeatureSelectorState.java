package com.google.android.gnd.ui.home;

import androidx.annotation.Nullable;
import com.google.android.gnd.model.feature.Feature;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;

public class FeatureSelectorState {
  public enum Visibility {
    VISIBLE,
    HIDDEN
  }

  private final FeatureSelectorState.Visibility visibility;
  private final Optional<ImmutableList<Feature>> features;

  private FeatureSelectorState(FeatureSelectorState.Visibility visibility, @Nullable ImmutableList<Feature> features) {
    this.visibility = visibility;
    this.features = Optional.ofNullable(features);
  }

  private FeatureSelectorState(FeatureSelectorState.Visibility visibility) {
    this(visibility, null);
  }

  public static FeatureSelectorState visible(ImmutableList<Feature> features) {
    return new FeatureSelectorState(FeatureSelectorState.Visibility.VISIBLE, features);
  }

  public static FeatureSelectorState hidden() {
    return new FeatureSelectorState(FeatureSelectorState.Visibility.HIDDEN);
  }

  public Optional<ImmutableList<Feature>> getFeatures() {
    return features;
  }

  public FeatureSelectorState.Visibility getVisibility() {
    return visibility;
  }

  public boolean isVisible() {
    return FeatureSelectorState.Visibility.VISIBLE.equals(visibility);
  }
}
