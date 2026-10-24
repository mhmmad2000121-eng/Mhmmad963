package com.example.ui.screens.vault

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.sensor.SensorTriggerState
import com.example.ui.theme.AccentRed
import com.example.ui.theme.CyanPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMainScreen(
    sensorState: SensorTriggerState,
    onSimulateTrigger: () -> Unit,
    onLockVault: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Intercept back button to return to Camouflage Notepad
    BackHandler {
        onLockVault()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CyanPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.vault_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.vault_subtitle),
                                fontSize = 11.sp,
                                color = CyanPrimary
                            )
                        }
                    }
                },
                actions = {
                    // Emergency lock and disguise button
                    IconButton(
                        onClick = onLockVault,
                        modifier = Modifier.testTag("lock_vault_btn")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentRed.copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.LockReset,
                                    contentDescription = stringResource(R.string.lock_vault),
                                    tint = AccentRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_media), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("tab_media_btn")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_apps), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("tab_apps_btn")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Sensors, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_sensor), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary
                    ),
                    modifier = Modifier.testTag("tab_sensor_btn")
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> MediaVaultScreen()
                1 -> AppBlockerScreen()
                2 -> SensorDiagnosticsScreen(
                    sensorState = sensorState,
                    onSimulateTrigger = onSimulateTrigger
                )
            }
        }
    }
}
