/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gnd.persistence.local;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.TestApplication;
import com.google.android.gnd.inject.DaggerTestComponent;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.model.basemap.tile.Tile.State;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Field.Type;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class LocalDataStoreTest {

  @Inject LocalDataStore localDataStore;

  @Before
  public void setUp() {
    DaggerTestComponent.create().inject(this);
  }

  @Test
  @Ignore("Inserting into table 'layer' (which contains foreign key) is causing an error")
  public void testInsertProject() {
    Field field =
        Field.newBuilder()
            .setId("field id")
            .setLabel("field label")
            .setRequired(false)
            .setType(Type.TEXT)
            .build();

    Element element = Element.ofField(field);

    Form form =
        Form.newBuilder()
            .setId("form id")
            .setElements(ImmutableList.<Element>builder().add(element).build())
            .build();

    Layer layer =
        Layer.newBuilder()
            .setId("layer id")
            .setItemLabel("item label")
            .setListHeading("heading title")
            .setDefaultStyle(Style.builder().setColor("000").build())
            .setForm(form)
            .build();

    Project.Builder builder =
        Project.newBuilder()
            .setId("project id")
            .setTitle("project 1")
            .setDescription("foo description");
    builder.putLayer("layer id", layer);

    // TODO: Fix this test and include again
    localDataStore.insertOrUpdateProject(builder.build()).test().assertComplete();
  }

  @Test
  public void testGetProject() {
    Project project1 =
        Project.newBuilder()
            .setId("id 1")
            .setTitle("project 1")
            .setDescription("foo description")
            .build();
    Project project2 =
        Project.newBuilder()
            .setId("id 2")
            .setTitle("project 2")
            .setDescription("foo description 2")
            .build();
    localDataStore.insertOrUpdateProject(project1).test().assertComplete();
    localDataStore.insertOrUpdateProject(project2).test().assertComplete();
    localDataStore
        .getProjects()
        .test()
        .assertValue(ImmutableList.<Project>builder().add(project1, project2).build());
  }

  @Test
  public void testGetProjectById() {
    Project project =
        Project.newBuilder()
            .setId("foo id 2")
            .setTitle("project 2")
            .setDescription("foo description 2")
            .build();
    localDataStore.insertOrUpdateProject(project).test().assertComplete();
    localDataStore.getProjectById("foo id 2").test().assertValue(project);
  }

  @Test
  public void testDeleteProject() {
    Project project1 =
        Project.newBuilder()
            .setId("foo id")
            .setTitle("project 1")
            .setDescription("foo description")
            .build();
    Project project2 =
        Project.newBuilder()
            .setId("foo id 2")
            .setTitle("project 2")
            .setDescription("foo description 2")
            .build();
    localDataStore.insertOrUpdateProject(project1).test().assertComplete();
    localDataStore.insertOrUpdateProject(project2).test().assertComplete();
    localDataStore.deleteProject(project1).test().assertComplete();
    localDataStore
        .getProjects()
        .test()
        .assertValue(ImmutableList.<Project>builder().add(project2).build());
  }

  @Test
  public void testInsertOrUpdateUser() {
    User user =
        User.builder()
            .setId("some id")
            .setDisplayName("test user")
            .setEmail("test@gmail.com")
            .build();
    localDataStore.insertOrUpdateUser(user).test().assertComplete();
    localDataStore.getUser("some id").test().assertValue(user);
  }

  @Test
  public void testGetTile() {
    Tile tile =
        Tile.newBuilder()
            .setId("tile id_1")
            .setPath("/foo/path1")
            .setState(State.PENDING)
            .setUrl("foo_url")
            .build();
    localDataStore.insertOrUpdateTile(tile).test().assertComplete();
    localDataStore.getTile("tile id_1").test().assertValueCount(1).assertValue(tile);

    tile = tile.toBuilder().setPath("/foo/path2").build();
    localDataStore.insertOrUpdateTile(tile).test().assertComplete();
    localDataStore.getTile("tile id_1").test().assertValueCount(1).assertValue(tile);
  }

  @Test
  public void testGetPendingTile() {
    Tile tile1 =
        Tile.newBuilder()
            .setId("id_1")
            .setState(State.PENDING)
            .setPath("some_path")
            .setUrl("some_url")
            .build();
    Tile tile2 =
        Tile.newBuilder()
            .setId("id_2")
            .setState(State.DOWNLOADED)
            .setPath("some_path")
            .setUrl("some_url")
            .build();
    Tile tile3 =
        Tile.newBuilder()
            .setId("id_3")
            .setState(State.PENDING)
            .setPath("some_path")
            .setUrl("some_url")
            .build();
    localDataStore.insertOrUpdateTile(tile1).test().assertComplete();
    localDataStore.insertOrUpdateTile(tile2).test().assertComplete();
    localDataStore.insertOrUpdateTile(tile3).test().assertComplete();

    localDataStore
        .getPendingTiles()
        .test()
        .assertValue(ImmutableList.<Tile>builder().add(tile1, tile3).build());
  }

  @Test
  public void testInsertOrUpdateOfflineAreas() {
    LatLngBounds bounds1 = LatLngBounds.builder().include(new LatLng(0.0, 0.0)).build();
    OfflineArea area1 =
        OfflineArea.newBuilder()
            .setId("id_1")
            .setBounds(bounds1)
            .setState(OfflineArea.State.PENDING)
            .build();
    LatLngBounds bounds2 = LatLngBounds.builder().include(new LatLng(10.0, 30.0)).build();
    OfflineArea area2 =
        OfflineArea.newBuilder()
            .setId("id_2")
            .setBounds(bounds2)
            .setState(OfflineArea.State.PENDING)
            .build();

    localDataStore.insertOrUpdateOfflineArea(area1).test().assertComplete();
    localDataStore.insertOrUpdateOfflineArea(area2).test().assertComplete();
    localDataStore
        .getOfflineAreas()
        .test()
        .assertValue(ImmutableList.<OfflineArea>builder().add(area1, area2).build());
  }
}
