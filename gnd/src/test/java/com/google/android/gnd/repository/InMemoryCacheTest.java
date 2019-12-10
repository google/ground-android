package com.google.android.gnd.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.Layer;
import org.junit.Before;
import org.junit.Test;

public class InMemoryCacheTest {

  private InMemoryCache inMemoryCache;
  private Project project;
  private Feature feature;

  @Before
  public void setUp() {
    inMemoryCache = new InMemoryCache();

    project =
        Project.newBuilder()
            .setId("foo project id")
            .setTitle("foo title")
            .setDescription("foo description")
            .build();

    feature =
        Feature.newBuilder()
            .setId("foo feature id")
            .setProject(Project.newBuilder().build())
            .setLayer(Layer.newBuilder().build())
            .setPoint(Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build())
            .build();
  }

  @Test
  public void testSaveFeatures() {
    assertEquals(0, inMemoryCache.getFeatures().size());

    inMemoryCache.putFeature(feature);

    assertEquals(1, inMemoryCache.getFeatures().size());
    assertEquals(feature, inMemoryCache.getFeatures().asList().get(0));
  }

  @Test
  public void testDeleteFeatures() {
    inMemoryCache.putFeature(feature);

    assertEquals(1, inMemoryCache.getFeatures().size());

    // remove feature
    inMemoryCache.removeFeature(feature.getId());

    assertEquals(0, inMemoryCache.getFeatures().size());
  }

  @Test
  public void testSaveActiveProject() {
    inMemoryCache.putFeature(feature);

    assertEquals(1, inMemoryCache.getFeatures().size());
    assertNull(inMemoryCache.getActiveProject());

    // save project
    inMemoryCache.setActiveProject(project);

    assertEquals(project, inMemoryCache.getActiveProject());
    assertEquals(0, inMemoryCache.getFeatures().size());
  }

  @Test
  public void testRemoveActiveProject() {
    inMemoryCache.setActiveProject(project);
    inMemoryCache.putFeature(feature);

    assertNotNull(inMemoryCache.getActiveProject());
    assertEquals(1, inMemoryCache.getFeatures().size());

    // remove saved project
    inMemoryCache.clearActiveProject();

    assertNull(inMemoryCache.getActiveProject());
    assertEquals(0, inMemoryCache.getFeatures().size());
  }
}
