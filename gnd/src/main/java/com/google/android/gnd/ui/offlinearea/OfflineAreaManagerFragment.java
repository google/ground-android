package com.google.android.gnd.ui.offlinearea;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gnd.MainActivity;
import com.google.android.gnd.databinding.OfflineAreaManagerFragBinding;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.TwoLineToolbar;

import butterknife.BindView;

@ActivityScoped
public class OfflineAreaManagerFragment extends AbstractFragment {

  @BindView(R.id.offline_maps_toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.offline_maps_list)
  RecyclerView areaList;

  private OfflineAreaManagerViewModel viewModel;

  public static OfflineAreaManagerFragment newInstance() {
    return new OfflineAreaManagerFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(OfflineAreaManagerViewModel.class);
    // TODO: use the viewmodel
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    OfflineAreaManagerFragBinding binding =
        OfflineAreaManagerFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    toolbar.setTitle("Offline Maps");
    ((MainActivity) getActivity()).setActionBar(toolbar);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }
}
