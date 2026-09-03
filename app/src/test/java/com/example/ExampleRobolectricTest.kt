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
    assertEquals("AI Language Translation Tool", appName)
  }

  @Test
  fun `activity launches successfully`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.setup().get()
    org.junit.Assert.assertNotNull(activity)
  }

  @Test
  fun `language catalog has over 50 languages including Indian and global`() {
    org.junit.Assert.assertTrue(com.example.model.LanguageCatalog.ALL_LANGUAGES.size >= 50)
    org.junit.Assert.assertNotNull(com.example.model.LanguageCatalog.findByCode("ta"))
    org.junit.Assert.assertNotNull(com.example.model.LanguageCatalog.findByCode("hi"))
    org.junit.Assert.assertNotNull(com.example.model.LanguageCatalog.findByCode("es"))
    org.junit.Assert.assertNotNull(com.example.model.LanguageCatalog.findByCode("ja"))
  }

  @Test
  fun `download manager initializes with default downloaded languages`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.LanguageDownloadManager(context)
    val downloaded = manager.getDownloadedLanguageCodes()
    org.junit.Assert.assertTrue(downloaded.contains("en"))
    org.junit.Assert.assertTrue(downloaded.contains("ta"))
  }
}
