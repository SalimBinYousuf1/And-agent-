package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ParsedModelResponse
import com.example.domain.model.AgentAction
import com.example.domain.resolver.ActionResolver
import com.example.domain.resolver.ResolutionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("salim", appName)
    }

    @Test
    fun `test action resolver resolves sms correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = ActionResolver(context)

        val rawResponse = ParsedModelResponse(
            action = "SEND_SMS",
            recipientName = "Mom",
            phoneNumber = "+1234567890",
            message = "Running 10 mins late"
        )

        val result = resolver.resolve(
            rawResponse = rawResponse,
            directSmsOptIn = false,
            directCallOptIn = false,
            exactAlarmOptIn = false
        )

        assertTrue(result is ResolutionResult.Resolved)
        val action = (result as ResolutionResult.Resolved).action
        assertTrue(action is AgentAction.SendSms)
        val sms = action as AgentAction.SendSms
        assertEquals("+1234567890", sms.phoneNumber)
        assertEquals("Running 10 mins late", sms.message)
    }

    @Test
    fun `test action resolver resolves alarm correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = ActionResolver(context)

        val rawResponse = ParsedModelResponse(
            action = "SET_ALARM",
            hour = 7,
            minute = 30,
            message = "Wake up"
        )

        val result = resolver.resolve(
            rawResponse = rawResponse,
            directSmsOptIn = false,
            directCallOptIn = false,
            exactAlarmOptIn = true
        )

        assertTrue(result is ResolutionResult.Resolved)
        val action = (result as ResolutionResult.Resolved).action
        assertTrue(action is AgentAction.SetAlarm)
        val alarm = action as AgentAction.SetAlarm
        assertEquals(7, alarm.hour)
        assertEquals(30, alarm.minute)
        assertEquals("Wake up", alarm.message)
        assertTrue(alarm.isExact)
    }
}

