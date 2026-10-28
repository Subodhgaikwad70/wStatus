package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Status Saver", appName)
  }

  @Test
  fun `status item format size and duration`() {
    val item = com.example.model.StatusItem(
      id = "test_1",
      uriString = "content://test",
      title = "Test Video",
      mediaType = com.example.model.MediaType.VIDEO,
      sizeBytes = 2 * 1024 * 1024L,
      dateModified = 1698450000000L,
      appType = com.example.model.WhatsAppAppType.WHATSAPP,
      durationMs = 65000L
    )
    assertEquals("2.0 MB", item.formattedSize)
    assertEquals("01:05", item.formattedDuration)
    org.junit.Assert.assertTrue(item.formattedDateTime.isNotEmpty())
    org.junit.Assert.assertTrue(item.formattedTime.isNotEmpty())
  }
}
