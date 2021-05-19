package com.google.android.gnd.ui.home.featureselector;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;
import static com.google.common.base.Preconditions.checkNotNull;
import static java8.util.stream.StreamSupport.stream;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.FeatureSelectorDialogBinding;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.ui.common.AbstractDialogFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.AndroidEntryPoint;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;

@AndroidEntryPoint
public class FeatureSelectorFragment extends AbstractDialogFragment {
  @Inject EphemeralPopups popups;

  private FeatureSelectorViewModel viewModel;

  @SuppressWarnings("NullAway")
  private FeatureSelectorDialogBinding binding;

  @Nullable private ArrayAdapter listAdapter;

  public FeatureSelectorFragment(FeatureSelectorViewModel featureSelectorViewModel) {
    this.viewModel = featureSelectorViewModel;
  }

  // public FeatureSelectorFragment(FeatureSelectorViewModel vm) {
  //  this.viewModel = vm;
  // }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public @NotNull Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);
    AlertDialog.Builder dialog = new Builder(getContext());
    dialog.setTitle("Select Feature");
    LayoutInflater inflater = getActivity().getLayoutInflater();
    binding = FeatureSelectorDialogBinding.inflate(inflater);
    listAdapter =
        new ArrayAdapter(getContext(), R.layout.feature_selector_list_item, R.id.feature_name);
    binding.featureSelectorListView.setAdapter(listAdapter);
    binding.featureSelectorListView.setOnItemClickListener(
        (parent, view, index, id) -> onItemSelected(index));
    viewModel
        .getFeatures()
        .as(autoDisposable(getParentFragment()))
        .subscribe(this::showFeatureList);
    dialog.setView(binding.getRoot());
    dialog.setCancelable(true);
    return dialog.create();
  }

  private void showFeatureList(ImmutableList<Feature> features) {
    binding.listLoadingProgressBar.setVisibility(View.GONE);
    checkNotNull(listAdapter, "listAdapter was null when attempting to show project list");

    listAdapter.clear();
    stream(features).map(this::setFeatureText).forEach(listAdapter::add);
    if (features.size() == 1) {
      onItemSelected(0);
    }
    binding.featureSelectorListView.setVisibility(View.VISIBLE);
  }

  private String setFeatureText(Feature feature) {
    String text = "";
    if (feature.isGeoJson()) {
      text = "Area\n";
    } else if (feature.isPoint()) {
      text = "Point\n";
    }

    return text + feature.getLayer().getName();
  }

  private void onItemSelected(int index) {
    dismiss();
    viewModel.selectFeature(index);
  }
}
