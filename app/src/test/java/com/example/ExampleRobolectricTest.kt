package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.VaultSecurityController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals("Ghost Vault", appName)
    }

    @Test
    fun `verify security controller block and authorization`() {
        val testPkg = "com.test.secretapp"
        VaultSecurityController.setPackageBlocked(testPkg, true)
        assertTrue(VaultSecurityController.isPackageBlocked(testPkg))

        // Before authorization, it is not authorized
        assertFalse(VaultSecurityController.isLaunchAuthorized(testPkg))

        // Authorize secure launch
        VaultSecurityController.authorizeSecureLaunch(testPkg)
        assertTrue(VaultSecurityController.isLaunchAuthorized(testPkg))

        // Remove block
        VaultSecurityController.setPackageBlocked(testPkg, false)
        assertFalse(VaultSecurityController.isPackageBlocked(testPkg))
    }
}
