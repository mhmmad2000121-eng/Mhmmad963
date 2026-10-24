package com.example.service

import android.content.Context
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object VaultSecurityController {
    // Map of packageName to authorized launch expiry timestamp (epoch ms)
    private val authorizedLaunches = ConcurrentHashMap<String, Long>()

    // In-memory cache of currently blocked packages for fast lookup in AccessibilityService
    private val blockedPackages = ConcurrentHashMap.newKeySet<String>()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked = _isVaultUnlocked.asStateFlow()

    fun setVaultUnlocked(unlocked: Boolean) {
        _isVaultUnlocked.value = unlocked
    }

    /**
     * Initializes the blocked packages cache from the database.
     */
    fun syncWithDatabase(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val blocked = db.vaultDao().getAllBlockedApps().first()
            blockedPackages.clear()
            blocked.filter { it.isBlocked }.forEach { blockedPackages.add(it.packageName) }
        }
    }

    fun isPackageBlocked(packageName: String): Boolean {
        return blockedPackages.contains(packageName)
    }

    fun setPackageBlocked(packageName: String, blocked: Boolean) {
        if (blocked) {
            blockedPackages.add(packageName)
        } else {
            blockedPackages.remove(packageName)
            authorizedLaunches.remove(packageName)
        }
    }

    /**
     * Grants a 20-second temporary launch window so the user can open the app
     * directly from inside the Ghost Vault without the accessibility service intercepting it.
     */
    fun authorizeSecureLaunch(packageName: String) {
        authorizedLaunches[packageName] = System.currentTimeMillis() + 20_000L
    }

    fun isLaunchAuthorized(packageName: String): Boolean {
        val expiry = authorizedLaunches[packageName] ?: return false
        if (System.currentTimeMillis() <= expiry) {
            return true
        }
        authorizedLaunches.remove(packageName)
        return false
    }
}
