package com.google.android.gnd.ui.home.mapcontainer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.google.android.gnd.databinding.MapTypeItemBinding;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel.MapTypeItem;
import java.util.List;

/**
 * Adapter for displaying map types.
 */
public class MapTypesAdapter extends BaseAdapter {
  private final List<MapTypeItem> mapTypes;

  MapTypesAdapter(List<MapTypeItem> mapTypes) {
    this.mapTypes = mapTypes;
  }

  @Override
  public int getCount() {
    return mapTypes.size();
  }

  @Override
  public Object getItem(int i) {
    return mapTypes.get(i).getType();
  }

  @Override
  public long getItemId(int i) {
    return i;
  }

  @Override
  public View getView(int i, View view, ViewGroup viewGroup) {
    MapTypeItemBinding binding =
        MapTypeItemBinding.inflate(LayoutInflater.from(viewGroup.getContext()));
    MapTypeItem mapType = mapTypes.get(i);
    binding.mapTypeTextView.setChecked(mapType.isSelected());
    binding.mapTypeTextView.setText(mapType.getLabel());
    return binding.getRoot();
  }
}
