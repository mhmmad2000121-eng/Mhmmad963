package com.example.service

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

class GhostDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "تم تفعيل صلاحية إدارة الجهاز للخزنة الشبحية بنجاح", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "تم إلغاء تفعيل صلاحية إدارة الجهاز", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, GhostDeviceAdminReceiver::class.java)
        }

        fun isDeviceAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            return dpm?.isAdminActive(getComponentName(context)) == true
        }

        fun isDeviceOwner(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            return dpm?.isDeviceOwnerApp(context.packageName) == true
        }

        fun createEnableAdminIntent(context: Context): Intent {
            return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "صلاحية مطلوبة للتحكم في إخفاء التطبيقات جذرياً من شاشة النظام الرئيسية."
                )
            }
        }

        /**
         * Sets an application hidden or unhidden using DevicePolicyManager.
         * Returns Result indicating success or exception message (e.g. requires Device Owner).
         */
        fun setApplicationHidden(context: Context, packageName: String, hidden: Boolean): Result<Boolean> {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return Result.failure(IllegalStateException("DevicePolicyManager not available"))

            return try {
                val admin = getComponentName(context)
                val success = dpm.setApplicationHidden(admin, packageName, hidden)
                Result.success(success)
            } catch (e: SecurityException) {
                // Not device owner or profile owner
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
