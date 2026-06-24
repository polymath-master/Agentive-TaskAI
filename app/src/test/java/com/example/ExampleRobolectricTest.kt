package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.storage.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    assertEquals("Agentive TaskAI", appName)
  }

  @Test
  fun `test preferences manager persists and loads settings`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pm = PreferencesManager(context)

    // Save values
    pm.saveSheetUrl("https://docs.google.com/spreadsheets/d/test_sheet")
    pm.saveTemplateDocUrl("https://docs.google.com/document/d/test_doc")

    // Retrieve values
    val savedSheet = pm.sheetUrlFlow.first()
    val savedDoc = pm.templateDocUrlFlow.first()

    assertEquals("https://docs.google.com/spreadsheets/d/test_sheet", savedSheet)
    assertEquals("https://docs.google.com/document/d/test_doc", savedDoc)
  }
}
