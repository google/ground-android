package org.groundplatform.android.e2etest.tests

import android.Manifest
import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.takeScreenshot
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import java.io.IOException
import org.groundplatform.android.e2etest.TestConfig
import org.groundplatform.android.e2etest.drivers.AndroidTestDriver
import org.groundplatform.android.e2etest.robots.DataCollectionRobot
import org.groundplatform.android.e2etest.robots.HomeScreenRobot
import org.groundplatform.android.e2etest.robots.SignInRobot
import org.groundplatform.android.e2etest.robots.SurveySelectorRobot
import org.groundplatform.android.e2etest.robots.TermsOfServiceRobot
import org.groundplatform.android.ui.main.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompleteAllTaskTypesTest {

  @get:Rule var nameRule = TestName()

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @get:Rule
  var screenshotRule =
    object : TestWatcher() {
      override fun failed(e: Throwable?, description: Description?) {
        super.failed(e, description)
        captureScreenshot()
      }
    }

  @get:Rule
  val runtimePermissionsRule: GrantPermissionRule =
    GrantPermissionRule.grant(
      Manifest.permission.CAMERA,
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

  private lateinit var testDriver: AndroidTestDriver

  @Before
  fun setup() {
    Intents.init()
    testDriver =
      AndroidTestDriver(
        composeRule = composeTestRule,
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()),
      )
  }

  @After
  fun tearDown() {
    Intents.release()
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
  }

  private fun captureScreenshot() {
    val screenShotName = "${javaClass.simpleName}_${nameRule.methodName}"
    Log.d("Screenshots", "Taking screenshot of '$screenShotName'")
    try {
      takeScreenshot().writeToTestStorage(screenShotName)
    } catch (ex: IOException) {
      Log.e("Screenshots", "Could not take the screenshot", ex)
    }
  }
}
