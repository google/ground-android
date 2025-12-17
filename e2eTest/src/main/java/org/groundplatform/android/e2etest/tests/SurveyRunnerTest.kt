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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SurveyRunnerTest {

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
    with(HomeScreenRobot(testDriver)) {
      moveMap()
      recenter()
      addLoi()
      selectJob(TestConfig.JOB_TEST_EACH_TASK_TYPE)
      acceptDataSharingTerms()
    }
    with(DataCollectionRobot(testDriver)) {
      dismissInstructions()
      completeSurvey(TestConfig.JOB_TEST_EACH_TASK_TYPE_LIST)
    }
    // TODO replace this with deleting the submission
    with(HomeScreenRobot(testDriver)) {
      moveMap()
      recenter()
    }
  }
}
