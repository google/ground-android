package com.google.android.gnd;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MyTest {

  @Test
  public void testThisRuns(){
    int a = 1;
    assertEquals(a, 1);
  }
}
