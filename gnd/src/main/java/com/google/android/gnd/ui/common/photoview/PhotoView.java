package com.google.android.gnd.ui.common.photoview;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import androidx.fragment.app.Fragment;
import com.google.android.gnd.databinding.PhotoViewBinding;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.system.StorageManager;
import com.google.android.material.card.MaterialCardView;
import javax.annotation.Nullable;
import javax.inject.Inject;

public class PhotoView extends MaterialCardView {

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

    LayoutInflater inflater =
        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    binding = PhotoViewBinding.inflate(inflater);
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

  public interface Callback {
    void run(Uri uri);
  }
}
