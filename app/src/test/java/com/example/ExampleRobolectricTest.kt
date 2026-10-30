package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.SmartTransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertEquals("Household CFO", appName)
    }

    @Test
    fun `smart transaction parser parses UPI debit correctly`() {
        val sms = "Rs 450.00 spent on your Card ending 1234 at SWIGGY on 28-OCT-2026."
        val parsed = SmartTransactionParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(450.0, parsed?.amount ?: 0.0, 0.01)
        assertEquals("EXPENSE", parsed?.type)
        assertTrue(parsed?.merchant?.contains("SWIGGY", ignoreCase = true) == true)
    }

    @Test
    fun `smart transaction parser parses salary credit correctly`() {
        val sms = "Your account has been credited with INR 85,000.00 on 01-OCT-2026 by TECH CORP."
        val parsed = SmartTransactionParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(85000.0, parsed?.amount ?: 0.0, 0.01)
        assertEquals("INCOME", parsed?.type)
    }
}
