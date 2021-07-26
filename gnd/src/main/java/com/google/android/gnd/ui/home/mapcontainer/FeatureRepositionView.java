package com.google.android.gnd.ui.home.mapcontainer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.MapMoveFeatureLayoutBinding;

public class FeatureRepositionView extends FrameLayout {

  private MapMoveFeatureLayoutBinding binding;

  public FeatureRepositionView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private LayoutInflater getLayoutInflater() {
    return (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  private void init() {
    LayoutInflater inflater = getLayoutInflater();
    binding = DataBindingUtil.inflate(inflater, R.layout.map_move_feature_layout, this, true);
  }

  public void setViewModel(FeatureRepositionViewModel viewModel) {
    binding.setViewModel(viewModel);
  }
}
