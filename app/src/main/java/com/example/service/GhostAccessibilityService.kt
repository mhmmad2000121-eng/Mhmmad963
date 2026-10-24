package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class GhostAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastInterceptedPackage: String? = null
    private var lastInterceptTime: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return

        // Skip checking if it's this app itself or system launcher
        if (pkgName == packageName || pkgName == "com.android.launcher" || pkgName == "com.google.android.apps.nexuslauncher") {
            return
        }

        if (VaultSecurityController.isPackageBlocked(pkgName)) {
            // Check if user launched this app securely from inside Ghost Vault
            if (VaultSecurityController.isLaunchAuthorized(pkgName)) {
                // Authorized launch, allow it
                return
            }

            // Outside launch detected! Perform immediate return to home
            val now = System.currentTimeMillis()
            if (pkgName != lastInterceptedPackage || (now - lastInterceptTime) > 3000L) {
                lastInterceptedPackage = pkgName
                lastInterceptTime = now

                // Force return to home screen
                performGlobalAction(GLOBAL_ACTION_HOME)

                mainHandler.post {
                    Toast.makeText(
                        applicationContext,
                        "🔒 تم حظر تشغيل هذا التطبيق بواسطة الخزنة الشبحية!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    override fun onInterrupt() {}
}
