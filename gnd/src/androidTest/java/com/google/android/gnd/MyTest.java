package com.google.android.gnd;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import com.google.android.gnd.inject.DaggerTestGndApplicationComponent;
import com.google.android.gnd.inject.TestGndApplicationComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
@LargeTest
public class MyTest {

  @Test
  public void testThisRuns(){
    int a = 1;
    assertEquals(a, 1);
  }


  @Test
  public void mainScreen_FABIsDisplayed() {

    // Build an instance of our test graph - application provider comes from test framework

    // We create a new graph each time so that there are no shared objects between tests
    // Performance cost? evaluation is lazy and done at compile time so tests should be fast

    TestGndApplication app = ApplicationProvider.getApplicationContext();
    TestGndApplicationComponent graph = (TestGndApplicationComponent) DaggerTestGndApplicationComponent
      .factory().create(app);
    graph.inject(app);

    ActivityScenario.launch(MainActivity.class);
    onView(withId(R.id.sign_in_button)).perform(click());
    onView(withId(R.id.add_observation_btn)).check(matches(isDisplayed()));
  }
}
