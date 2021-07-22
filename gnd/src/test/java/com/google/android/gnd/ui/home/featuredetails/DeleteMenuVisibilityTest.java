package com.google.android.gnd.ui.home.featuredetails;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.gnd.TestObservers;
import com.google.android.gnd.model.Role;
import org.junit.Test;

public class DeleteMenuVisibilityTest extends FeatureDetailsViewModelTest {

  @Test
  public void testDeleteMenuVisible_whenUserRoleIsOwnerAndFeatureIsPoint() {
    setUserAclAndSelectedFeature(Role.OWNER, TEST_POINT_FEATURE);
    TestObservers.observeUntilFirstChange(viewModel.getDeleteMenuOptionVisible());
    assertThat(viewModel.getDeleteMenuOptionVisible().getValue()).isTrue();
  }

  @Test
  public void testDeleteMenuVisible_whenUserRoleIsOwnerAndFeatureIsPolygon() {
    setUserAclAndSelectedFeature(Role.OWNER, TEST_POLYGON_FEATURE);
    TestObservers.observeUntilFirstChange(viewModel.getDeleteMenuOptionVisible());
    assertThat(viewModel.getDeleteMenuOptionVisible().getValue()).isTrue();
  }

  @Test
  public void testDeleteMenuVisible_whenUserRoleIsManager() {
    setUserAclAndSelectedFeature(Role.MANAGER, TEST_POINT_FEATURE);
    TestObservers.observeUntilFirstChange(viewModel.getDeleteMenuOptionVisible());
    assertThat(viewModel.getDeleteMenuOptionVisible().getValue()).isTrue();
  }

  @Test
  public void testDeleteMenuVisible_whenUserRoleIsContributor() {
    setUserAclAndSelectedFeature(Role.CONTRIBUTOR, TEST_POINT_FEATURE);
    TestObservers.observeUntilFirstChange(viewModel.getDeleteMenuOptionVisible());
    assertThat(viewModel.getDeleteMenuOptionVisible().getValue()).isFalse();
  }

  @Test
  public void testDeleteMenuVisible_whenUserRoleIsUnknown() {
    setUserAclAndSelectedFeature(Role.UNKNOWN, TEST_POINT_FEATURE);
    TestObservers.observeUntilFirstChange(viewModel.getDeleteMenuOptionVisible());
    assertThat(viewModel.getDeleteMenuOptionVisible().getValue()).isFalse();
  }
}
