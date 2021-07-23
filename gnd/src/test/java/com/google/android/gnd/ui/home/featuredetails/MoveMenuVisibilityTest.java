package com.google.android.gnd.ui.home.featuredetails;

import static com.google.android.gnd.model.Role.CONTRIBUTOR;
import static com.google.android.gnd.model.Role.MANAGER;
import static com.google.android.gnd.model.Role.OWNER;
import static com.google.android.gnd.model.Role.UNKNOWN;
import static com.google.android.gnd.model.feature.FeatureType.POINT;
import static com.google.android.gnd.model.feature.FeatureType.POLYGON;
import static com.google.common.truth.Truth.assertThat;

import com.google.android.gnd.TestObservers;
import com.google.android.gnd.model.Role;
import com.google.android.gnd.model.feature.FeatureType;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MoveMenuVisibilityTest extends FeatureDetailsViewModelTest {

  @Parameterized.Parameter()
  public Role userRole;

  @Parameterized.Parameter(1)
  public FeatureType featureType;

  @Parameterized.Parameter(2)
  public boolean result;

  @Parameterized.Parameters(name = "{index}: Test with role={0}, featureType={1}, result is={2} ")
  public static Collection<Object[]> data() {
    Object[][] data =
        new Object[][] {
          {OWNER, POINT, true},
          {MANAGER, POINT, true},
          {CONTRIBUTOR, POINT, false},
          {UNKNOWN, POINT, false},
          {OWNER, POLYGON, false},
          {MANAGER, POLYGON, false},
          {CONTRIBUTOR, POLYGON, false},
          {UNKNOWN, POLYGON, false},
        };
    return Arrays.asList(data);
  }

  @Test
  public void testMoveMenuVisible() {
    mockCurrentUserRole(userRole);
    setSelectedFeature(featureType);

    TestObservers.observeUntilFirstChange(viewModel.getMoveMenuOptionVisible());
    assertThat(viewModel.getMoveMenuOptionVisible().getValue()).isEqualTo(result);
  }
}
