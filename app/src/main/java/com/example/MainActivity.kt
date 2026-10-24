package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.sensor.SensorTriggerManager
import com.example.service.VaultSecurityController
import com.example.ui.screens.camouflage.CamouflageNotepadScreen
import com.example.ui.screens.vault.VaultMainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var sensorTriggerManager: SensorTriggerManager
    private var isVaultUnlockedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorTriggerManager = SensorTriggerManager(
            context = this,
            scope = lifecycleScope,
            onTriggerSuccess = {
                unlockVault()
            }
        )

        setContent {
            MyApplicationTheme {
                val sensorState by sensorTriggerManager.state.collectAsState()
                var isVaultUnlocked by remember { isVaultUnlockedState }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = isVaultUnlocked,
                        animationSpec = tween(durationMillis = 350),
                        label = "VaultScreenCrossfade"
                    ) { unlocked ->
                        if (unlocked) {
                            VaultMainScreen(
                                sensorState = sensorState,
                                onSimulateTrigger = {
                                    sensorTriggerManager.simulateTrigger()
                                },
                                onLockVault = {
                                    lockVault()
                                }
                            )
                        } else {
                            CamouflageNotepadScreen(
                                sensorState = sensorState,
                                onUnlockVault = {
                                    unlockVault()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun unlockVault() {
        isVaultUnlockedState.value = true
        VaultSecurityController.setVaultUnlocked(true)
        sensorTriggerManager.stopListening()
    }

    private fun lockVault() {
        isVaultUnlockedState.value = false
        VaultSecurityController.setVaultUnlocked(false)
        sensorTriggerManager.resetUnlockState()
    }

    override fun onResume() {
        super.onResume()
        if (!isVaultUnlockedState.value) {
            sensorTriggerManager.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorTriggerManager.stopListening()
    }
}
