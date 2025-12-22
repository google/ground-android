package org.groundplatform.android.e2etest.tests

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import org.groundplatform.android.e2etest.TestConfig
import org.groundplatform.android.e2etest.drivers.AndroidTestDriver
import org.groundplatform.android.e2etest.robots.DataCollectionRobot
import org.groundplatform.android.e2etest.robots.HomeScreenRobot
import org.groundplatform.android.e2etest.robots.SignInRobot
import org.groundplatform.android.e2etest.robots.SurveySelectorRobot
import org.groundplatform.android.e2etest.robots.TermsOfServiceRobot
import org.groundplatform.android.ui.main.MainActivity
import org.groundplatform.android.ui.map.gms.features.TEST_MARKER_TAG
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompleteAllTaskTypesTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @get:Rule
  val runtimePermissionsRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)

  private lateinit var testDriver: AndroidTestDriver

  @Before
  fun setup() {
    testDriver =
      AndroidTestDriver(
        composeRule = composeTestRule,
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()),
      )
  }

  @Test
  fun run() {
    SignInRobot(testDriver).signIn()
    TermsOfServiceRobot(testDriver).agree()
    with(SurveySelectorRobot(testDriver)) {
      expandPrivateSurveys()
      selectSurvey(TestConfig.SURVEY_NAME)
    }
    // Add new LOI and test all task types except DRAW_AREA
    with(HomeScreenRobot(testDriver)) {
      moveMap()
      recenter()
      addLoi()
      selectJob(TestConfig.TEST_JOB_ALL_TASK_TYPES_EXCEPT_DRAW_AREA)
      acceptDataSharingTerms()
    }
    with(DataCollectionRobot(testDriver)) {
      dismissInstructions()
      runTasks(TestConfig.TEST_LIST_ALL_TASK_TYPES_EXCEPT_DRAW_AREA)
    }
    // Remove previous LOI and add new one to test DRAW_AREA
    with(HomeScreenRobot(testDriver)) {
      moveMap()
      recenter()
      deleteLoi(TEST_MARKER_TAG)
      addLoi()
      selectJob(TestConfig.TEST_JOB_DRAW_AREA)
    }
    with(DataCollectionRobot(testDriver)) {
      dismissInstructions()
      runTasks(TestConfig.TEST_LIST_DRAW_AREA)
    }
  }
}
