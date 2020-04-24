package com.google.android.gnd.ui.editobservation.field;

import static com.google.android.gnd.ui.util.ViewUtil.assignGeneratedId;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.databinding.PhotoInputFieldBinding;
import com.google.android.gnd.databinding.TextInputFieldBinding;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.editobservation.EditObservationFragment;
import com.google.android.gnd.ui.editobservation.EditObservationViewModel;
import com.google.android.gnd.ui.editobservation.PhotoFieldViewModel;
import timber.log.Timber;

public class FieldFactory {

  private final LayoutInflater layoutInflater;
  private final ViewGroup root;
  private final EditObservationViewModel viewModel;
  private final EditObservationFragment editObservationFragment;
  private final ViewModelFactory viewModelFactory;

  public FieldFactory(
    LayoutInflater layoutInflater,
    ViewGroup root,
    EditObservationViewModel viewModel,
    EditObservationFragment editObservationFragment,
    ViewModelFactory viewModelFactory) {
    this.layoutInflater = layoutInflater;
    this.root = root;
    this.viewModel = viewModel;
    this.editObservationFragment = editObservationFragment;
    this.viewModelFactory = viewModelFactory;
  }

  private void addTextField(Field field) {
    TextInputFieldBinding binding = TextInputFieldBinding.inflate(layoutInflater, root, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(editObservationFragment);
    binding.setField(field);
    root.addView(binding.getRoot());
    assignGeneratedId(binding.getRoot().findViewById(R.id.text_input_edit_text));
  }

  private void addMultipleChoiceField(Field field) {
    MultipleChoiceInputFieldBinding binding =
        MultipleChoiceInputFieldBinding.inflate(layoutInflater, root, false);
    binding.setFragment(editObservationFragment);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(editObservationFragment);
    binding.setField(field);
    root.addView(binding.getRoot());
    assignGeneratedId(binding.getRoot().findViewById(R.id.multiple_choice_input_edit_text));
  }

  private void addPhotoField(Field field) {
    PhotoInputFieldBinding binding = PhotoInputFieldBinding.inflate(layoutInflater, root, false);
    binding.setLifecycleOwner(editObservationFragment);
    binding.setField(field);
    binding.setFragment(editObservationFragment);

    PhotoFieldViewModel photoFieldViewModel = viewModelFactory.create(PhotoFieldViewModel.class);
    photoFieldViewModel.init(field, viewModel.getResponses());
    binding.setViewModel(photoFieldViewModel);

    root.addView(binding.getRoot());
  }

  public void addField(Field field) {
    switch (field.getType()) {
      case TEXT:
        addTextField(field);
        break;
      case MULTIPLE_CHOICE:
        addMultipleChoiceField(field);
        break;
      case PHOTO:
        addPhotoField(field);
        break;
      default:
        Timber.w("Unimplemented field type: %s", field.getType());
        break;
    }
  }
}
