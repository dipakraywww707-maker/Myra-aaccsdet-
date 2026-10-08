package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.system.PhoneSystemManager
import com.example.data.system.SystemCommandResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals("Myra", appName)
    }

    @Test
    fun `test phone system command processing for torch`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val systemManager = PhoneSystemManager(context)

        val result = systemManager.processVoiceCommand("Torch chalu karo")
        assertTrue(result is SystemCommandResult.Handled)

        val batteryResult = systemManager.processVoiceCommand("battery kitna hai")
        assertTrue(batteryResult is SystemCommandResult.Handled)
    }
}
