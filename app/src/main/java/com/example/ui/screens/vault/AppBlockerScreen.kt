package com.example.ui.screens.vault

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.db.BlockedAppConfig
import com.example.service.GhostDeviceAdminReceiver
import com.example.service.VaultSecurityController
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.CyanPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val iconBitmap: Bitmap? = null,
    val isSystemApp: Boolean = false
)

@Composable
fun AppBlockerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val blockedAppsList by db.vaultDao().getAllBlockedApps().collectAsState(initial = emptyList())

    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterBlockedOnly by remember { mutableStateOf(false) }

    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isDeviceAdminActive by remember { mutableStateOf(false) }

    // Launcher for Device Admin enable prompt
    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDeviceAdminActive = GhostDeviceAdminReceiver.isDeviceAdminActive(context)
    }

    // Check service states
    fun checkServices() {
        isAccessibilityEnabled = checkAccessibilityServiceEnabled(context)
        isDeviceAdminActive = GhostDeviceAdminReceiver.isDeviceAdminActive(context)
    }

    LaunchedEffect(Unit) {
        checkServices()
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val list = mutableListOf<InstalledAppItem>()

            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) continue // skip our own app
                val label = info.loadLabel(pm).toString()
                val iconDrawable = try {
                    info.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                val bitmap = iconDrawable?.let { drawableToBitmap(it) }
                val isSystem = (info.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                list.add(
                    InstalledAppItem(
                        appName = label,
                        packageName = pkg,
                        iconBitmap = bitmap,
                        isSystemApp = isSystem
                    )
                )
            }
            list.sortBy { it.appName.lowercase() }
            installedApps = list
            isLoadingApps = false
        }
    }

    val blockedSet = remember(blockedAppsList) {
        blockedAppsList.filter { it.isBlocked }.map { it.packageName }.toSet()
    }

    val filteredApps = remember(installedApps, searchQuery, filterBlockedOnly, blockedSet) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesBlockFilter = !filterBlockedOnly || blockedSet.contains(app.packageName)
            matchesSearch && matchesBlockFilter
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Protection Service Status Cards
            Column(modifier = Modifier.padding(16.dp)) {
                // Accessibility Service Card (immediate protection without ADB)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAccessibilityEnabled)
                            AccentGreen.copy(alpha = 0.15f)
                        else
                            AccentAmber.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isAccessibilityEnabled) AccentGreen else AccentAmber,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حماية الوصول (بدون ADB)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isAccessibilityEnabled)
                                    "الحماية نشطة: يتم إغلاق التطبيقات المحظورة فوراً عند فتحها من الخارج"
                                else
                                    "مطلوب تفعيل خدمة الوصول للرصد الفوري للتطبيقات المحظورة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isAccessibilityEnabled) {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                modifier = Modifier.testTag("enable_accessibility_btn")
                            ) {
                                Text("تفعيل", fontSize = 12.sp, color = Color.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Device Admin Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = if (isDeviceAdminActive) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إخفاء النظام (Device Admin)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isDeviceAdminActive) "مدير الجهاز مفعّل" else "مطلوب لتطبيق setApplicationHidden",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isDeviceAdminActive) {
                            OutlinedButton(
                                onClick = {
                                    val intent = GhostDeviceAdminReceiver.createEnableAdminIntent(context)
                                    deviceAdminLauncher.launch(intent)
                                },
                                modifier = Modifier.testTag("enable_device_admin_btn")
                            ) {
                                Text("منح الصلاحية", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_apps), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("apps_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !filterBlockedOnly,
                    onClick = { filterBlockedOnly = false },
                    label = { Text("الكل (${installedApps.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = filterBlockedOnly,
                    onClick = { filterBlockedOnly = true },
                    label = { Text("المحظورة (${blockedSet.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            if (isLoadingApps) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyanPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("جارٍ جلب التطبيقات المثبتة على الهاتف...", fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isBlocked = blockedSet.contains(app.packageName)
                        AppItemCard(
                            app = app,
                            isBlocked = isBlocked,
                            onToggleBlock = { blocked ->
                                scope.launch(Dispatchers.IO) {
                                    db.vaultDao().setAppBlocked(
                                        BlockedAppConfig(
                                            packageName = app.packageName,
                                            appName = app.appName,
                                            isBlocked = blocked
                                        )
                                    )
                                    VaultSecurityController.setPackageBlocked(app.packageName, blocked)

                                    // Attempt DevicePolicyManager setApplicationHidden
                                    if (isDeviceAdminActive) {
                                        val result = GhostDeviceAdminReceiver.setApplicationHidden(
                                            context,
                                            app.packageName,
                                            blocked
                                        )
                                        result.onFailure {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "حظر الوصول نشط فورياً! (إخفاء النظام يتطلب Device Owner)",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            },
                            onLaunchSecurely = {
                                // Authorize temporary bypass window
                                VaultSecurityController.authorizeSecureLaunch(app.packageName)

                                // Unhide if hidden via admin
                                if (isDeviceAdminActive) {
                                    GhostDeviceAdminReceiver.setApplicationHidden(context, app.packageName, false)
                                }

                                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                    Toast.makeText(
                                        context,
                                        "تم تشغيل ${app.appName} بأمان من داخل الخزنة",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "تعذر تشغيل هذا التطبيق مباشرة",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppItemCard(
    app: InstalledAppItem,
    isBlocked: Boolean,
    onToggleBlock: (Boolean) -> Unit,
    onLaunchSecurely: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_item_${app.packageName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap.asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = CyanPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isBlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🔒 محظور ومخفي من الشاشة الرئيسية",
                        fontSize = 10.sp,
                        color = AccentRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Secure launch button if blocked
            if (isBlocked) {
                IconButton(
                    onClick = onLaunchSecurely,
                    modifier = Modifier.testTag("launch_securely_${app.packageName}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CyanPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Launch securely",
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Block toggle Switch
            Switch(
                checked = isBlocked,
                onCheckedChange = onToggleBlock,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentRed
                ),
                modifier = Modifier.testTag("block_switch_${app.packageName}")
            )
        }
    }
}

fun checkAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    val myPackage = context.packageName
    return enabledServices.any { it.resolveInfo.serviceInfo.packageName == myPackage }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth <= 0) 96 else drawable.intrinsicWidth.coerceAtMost(192)
    val height = if (drawable.intrinsicHeight <= 0) 96 else drawable.intrinsicHeight.coerceAtMost(192)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
