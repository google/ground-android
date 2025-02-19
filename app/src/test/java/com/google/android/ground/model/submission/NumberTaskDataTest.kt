package com.google.android.ground.model.submission

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class NumberTaskDataTest {

  @Test
  fun `value property should return correct double representation`() {
    val taskData = NumberTaskData("10.5")
    assertEquals(10.5, taskData.value, 0.0)
  }

  @Test
  fun `isEmpty should return true for empty string`() {
    val taskData = NumberTaskData("")
    assertTrue(taskData.isEmpty())
  }

  @Test
  fun `isEmpty should return false for non-empty string`() {
    val taskData = NumberTaskData("15")
    assertFalse(taskData.isEmpty())
  }

  @Test
  fun `fromNumber should return null for empty string`() {
    assertNull(NumberTaskData.fromNumber(""))
  }

  @Test
  fun `fromNumber should return null for invalid number`() {
    assertNull(NumberTaskData.fromNumber("abc"))
    assertNull(NumberTaskData.fromNumber("."))
    assertNull(NumberTaskData.fromNumber("1.2.3"))
  }

  @Test
  fun `fromNumber should return NumberTaskData for valid number`() {
    val taskData = NumberTaskData.fromNumber("20.3")
    assertNotNull(taskData)
    assertEquals("20.3", (taskData as NumberTaskData).number)
  }
}
