package com.google.android.gnd.ui.basemapselector;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.BasemapSelectorFragBinding;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.map.ExtentSelector;
import com.google.android.gnd.ui.map.MapProvider;

import javax.inject.Inject;

import io.reactivex.Single;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

/**
 * Allows the user to select specific areas on a map for offline display. Users can toggle sections of
 * the map to add or remove imagery. Upon selection, basemap tiles are queued for download. When 
 * deselected, they are removed from the device.
 */
@ActivityScoped
public class BasemapSelectorFragment extends AbstractFragment {

  private static final String TAG = BasemapSelectorFragment.class.getName();
  private static final String MAP_FRAGMENT_KEY = MapProvider.class.getName() + "#fragment";
  private BasemapSelectorViewModel viewModel;
  private MainViewModel mainViewModel;

  @Inject MapProvider mapProvider;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(BasemapSelectorViewModel.class);
    mainViewModel = getViewModel(MainViewModel.class);
    Single<ExtentSelector> mapAdapter = mapProvider.getExtentSelector();
    mapAdapter.as(autoDisposable(this)).subscribe(this::onMapReady);
  }

  private void onMapReady(ExtentSelector map) {
    map.renderExtentSelectionLayer();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    BasemapSelectorFragBinding binding =
        BasemapSelectorFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (savedInstanceState == null) {
      replaceFragment(R.id.map, mapProvider.getFragment());
    } else {
      mapProvider.restore(restoreChildFragment(savedInstanceState, MAP_FRAGMENT_KEY));
    }
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mainViewModel.getWindowInsets().observe(this, this::onApplyWindowInsets);
  }

  private void onApplyWindowInsets(WindowInsetsCompat windowInsets) {
    ViewCompat.onApplyWindowInsets(mapProvider.getFragment().getView(), windowInsets);
    // TODO: Once we add control UI elements, translate them based on the inset to avoid collision
    //  with the android navbar.
  }
}
