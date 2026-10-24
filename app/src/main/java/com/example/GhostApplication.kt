package com.example

import android.app.Application
import com.example.service.VaultSecurityController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GhostApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        VaultSecurityController.syncWithDatabase(this, appScope)
    }
}
