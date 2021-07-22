package com.google.android.gnd.ui.home.featuredetails;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.gnd.TestObservers;
import com.google.android.gnd.model.Role;
import org.junit.Test;

public class MoveMenuVisibilityTest extends FeatureDetailsViewModelTest {

  @Test
  public void testMoveMenuVisible_whenUserRoleIsOwnerAndFeatureIsPoint() {
    setUserAclAndSelectedFeature(Role.OWNER, TEST_POINT_FEATURE);
    TestObservers.observeUntilFirstChange(viewModel.getMoveMenuOptionVisible());
    assertThat(viewModel.getMoveMenuOptionVisible().getValue()).isTrue();
  }

  @Test
  public void testMoveMenuVisible_whenUserRoleIsManagerAndFeatureIsPoint() {
    setUserAclAndSelectedFeature(Role.MANAGER, TEST_POINT_FEATURE);
    TestObservers.observeUntilFirstChange(viewModel.getMoveMenuOptionVisible());
    assertThat(viewModel.getMoveMenuOptionVisible().getValue()).isTrue();
  }

  @Test
  public void testMoveMenuVisible_whenUserRoleIsContributorAndFeatureIsPoint() {
    setUserAclAndSelectedFeature(Role.CONTRIBUTOR, TEST_POINT_FEATURE);
    TestObservers.observeUntilFirstChange(viewModel.getMoveMenuOptionVisible());
    assertThat(viewModel.getMoveMenuOptionVisible().getValue()).isFalse();
  }

  @Test
  public void testMoveMenuVisible_whenUserRoleIsUnknownAndFeatureIsPoint() {
    setUserAclAndSelectedFeature(Role.UNKNOWN, TEST_POINT_FEATURE);
    TestObservers.observeUntilFirstChange(viewModel.getMoveMenuOptionVisible());
    assertThat(viewModel.getMoveMenuOptionVisible().getValue()).isFalse();
  }

  @Test
  public void testMoveMenuVisible_whenFeatureIsNotAPoint() {
    setUserAclAndSelectedFeature(Role.OWNER, TEST_POLYGON_FEATURE);
    TestObservers.observeUntilFirstChange(viewModel.getMoveMenuOptionVisible());
    assertThat(viewModel.getMoveMenuOptionVisible().getValue()).isFalse();
  }
}
