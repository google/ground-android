package com.google.android.gnd.ui.home.featuredetails;

import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.Role;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.model.feature.PolygonFeature;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ObservationRepository;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.ui.MarkerIconFactory;
import com.google.android.gnd.ui.common.FeatureHelper;
import com.google.android.gnd.ui.util.DrawableUtil;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class FeatureDetailsViewModelTest {

  private static final User TEST_USER =
      User.builder().setId("user_id").setEmail("user@gmail.com").setDisplayName("User").build();

  private static final Layer TEST_LAYER =
      Layer.newBuilder()
          .setId("layer id")
          .setName("heading title")
          .setDefaultStyle(Style.builder().setColor("000").build())
          .setForm(Optional.empty())
          .build();

  private static final Project TEST_PROJECT =
      Project.newBuilder()
          .setId("project id")
          .setTitle("project 1")
          .setDescription("foo description")
          .putLayer("layer id", TEST_LAYER)
          .build();

  static final PointFeature TEST_POINT_FEATURE =
      PointFeature.newBuilder()
          .setId("feature id")
          .setProject(TEST_PROJECT)
          .setLayer(TEST_LAYER)
          .setPoint(Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build())
          .setCreated(AuditInfo.now(TEST_USER))
          .setLastModified(AuditInfo.now(TEST_USER))
          .build();

  static final PolygonFeature TEST_POLYGON_FEATURE =
      PolygonFeature.builder()
          .setId("feature id")
          .setProject(TEST_PROJECT)
          .setLayer(TEST_LAYER)
          .setVertices(
              ImmutableList.of(
                  Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build(),
                  Point.newBuilder().setLatitude(10.0).setLongitude(10.0).build(),
                  Point.newBuilder().setLatitude(20.0).setLongitude(20.0).build()))
          .setCreated(AuditInfo.now(TEST_USER))
          .setLastModified(AuditInfo.now(TEST_USER))
          .build();

  @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock MarkerIconFactory mockMarkerIconFactory;
  @Mock DrawableUtil mockDrawableUtil;
  @Mock FeatureHelper mockFeatureHelper;
  @Mock FeatureRepository mockFeatureRepository;
  @Mock ObservationRepository mockObservationRepository;
  @Mock UserRepository mockUserRepository;

  FeatureDetailsViewModel viewModel;

  @Before
  public void setUp() {
    viewModel =
        new FeatureDetailsViewModel(
            mockMarkerIconFactory,
            mockDrawableUtil,
            mockFeatureHelper,
            mockFeatureRepository,
            mockObservationRepository,
            mockUserRepository);
  }

  private void mockCurrentUserRole(Role role) {
    when(mockUserRepository.getUserRole(TEST_PROJECT)).thenReturn(role);
  }

  void setUserAclAndSelectedFeature(Role role, Feature feature) {
    mockCurrentUserRole(role);
    viewModel.onSelectedFeature(Optional.of(feature));
  }
}
