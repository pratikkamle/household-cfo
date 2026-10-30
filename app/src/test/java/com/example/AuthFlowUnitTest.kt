package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AuthAccount
import com.example.data.sync.CloudSyncManager
import com.example.ui.components.CrashDiagnostics
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthFlowUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var cloudSyncManager: CloudSyncManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Application>()
        database = AppDatabase.getDatabase(context)
        cloudSyncManager = CloudSyncManager(context, database)
        CrashDiagnostics.clearFatalError()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial auth account is null or restored from clean state`() {
        val account = cloudSyncManager.authAccount.value
        // May be null or local_user depending on shared prefs, but must not crash
        assertTrue(account == null || account.email.isNotBlank())
    }

    @Test
    fun `setting authenticated user updates observable StateFlow and persists`() {
        val testAccount = AuthAccount(
            uid = "google_test_12345",
            email = "tester@householdcfo.in",
            displayName = "Test User",
            isGoogle = true,
            photoUrl = "https://example.com/avatar.jpg"
        )

        cloudSyncManager.setAuthenticatedUser(testAccount)

        val current = cloudSyncManager.authAccount.value
        assertNotNull(current)
        assertEquals("google_test_12345", current?.uid)
        assertEquals("tester@householdcfo.in", current?.email)
        assertEquals("Test User", current?.displayName)
        assertTrue(current?.isGoogle == true)
        assertEquals("https://example.com/avatar.jpg", current?.photoUrl)
    }

    @Test
    fun `signing out resets auth account and sync state`() = runTest {
        val testAccount = AuthAccount(
            uid = "google_test_logout",
            email = "logout@householdcfo.in",
            displayName = "Logout Tester",
            isGoogle = true
        )
        cloudSyncManager.setAuthenticatedUser(testAccount)
        assertEquals("google_test_logout", cloudSyncManager.authAccount.value?.uid)

        cloudSyncManager.signOut(backgroundScope) {}
        advanceUntilIdle()

        assertNull(cloudSyncManager.authAccount.value)
        assertNull(cloudSyncManager.currentUser.value)
    }

    @Test
    fun `CrashDiagnostics records auth diagnostic events without sensitive tokens`() {
        CrashDiagnostics.recordLog("AUTH", "Test auth step executed without secrets")
        val report = CrashDiagnostics.getDiagnosticReport()
        assertTrue(report.contains("Test auth step executed without secrets"))
        assertFalse(report.contains("password"))
        assertFalse(report.contains("id_token"))
    }

    @Test
    fun `checkAndLoadHouseholdForUser survives when cloud household does not exist`() = runTest {
        val testAccount = AuthAccount(
            uid = "new_user_no_household_999",
            email = "newbie@householdcfo.in",
            displayName = "Newbie",
            isGoogle = true
        )
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = FinanceViewModel(app)
        vm.cloudSyncManager.setAuthenticatedUser(testAccount)

        // Must execute cleanly without uncaught exception
        vm.checkAndLoadHouseholdForUser(testAccount)
        advanceUntilIdle()

        assertFalse(vm.isCheckingCloudHousehold.value)
        // User remains authenticated even if household doesn't exist yet
        assertEquals(testAccount.email, vm.authAccount.value?.email)
    }
}
