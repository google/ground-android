package com.google.android.gnd.ui.common.photoview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import androidx.fragment.app.Fragment;
import com.google.android.gnd.databinding.PhotoViewBinding;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.system.StorageManager;
import javax.annotation.Nullable;
import javax.inject.Inject;

public class PhotoView extends FrameLayout {

  private final PhotoViewBinding binding;

  @Inject StorageManager storageManager;

  @Nullable private Response response;

  public PhotoView(Context context) {
    this(context, null);
  }

  public PhotoView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public PhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    binding = PhotoViewBinding.inflate(LayoutInflater.from(context), this, true);
  }

  public void setResponse(@Nullable Response response) {
    this.response = response;
  }

  public void setFragment(Fragment fragment) {
    binding.setLifecycleOwner(fragment);
  }

  public void setViewModel(PhotoViewViewModel viewModel) {
    if (viewModel == null) {
      return;
    }
    viewModel.setResponse(response);
    binding.setViewModel(viewModel);
  }
}
