package com.remodex.mobile.services

import com.remodex.mobile.core.model.CodexPairingQRPayload
import com.remodex.mobile.core.model.CodexTrustedMacRecord
import com.remodex.mobile.data.CodexRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal object EmptyTrustedDeviceRepositoryBindings {
    val trustedDevices: StateFlow<List<CodexTrustedMacRecord>> = MutableStateFlow(emptyList())
    val switchingDeviceId: StateFlow<String?> = MutableStateFlow(null)
    val deviceSwitchNotice: StateFlow<String?> = MutableStateFlow(null)
    val currentTrustedMacDeviceId: StateFlow<String?> = MutableStateFlow(null)
    val previousTrustedMacDeviceId: StateFlow<String?> = MutableStateFlow(null)
    val relayMacDeviceId: StateFlow<String?> = MutableStateFlow(null)

    suspend fun switchToTrustedDevice(deviceId: String) = Unit

    suspend fun switchToScannedDevice(payload: CodexPairingQRPayload) = Unit

    suspend fun cancelDeviceSwitch() = Unit

    fun setDeviceMenuVisible(
        deviceId: String,
        visible: Boolean,
    ) = Unit
}

internal interface EmptyTrustedDeviceCodexRepository : CodexRepository {
    override val trustedDevices: StateFlow<List<CodexTrustedMacRecord>>
        get() = EmptyTrustedDeviceRepositoryBindings.trustedDevices
    override val switchingDeviceId: StateFlow<String?>
        get() = EmptyTrustedDeviceRepositoryBindings.switchingDeviceId
    override val deviceSwitchNotice: StateFlow<String?>
        get() = EmptyTrustedDeviceRepositoryBindings.deviceSwitchNotice
    override val currentTrustedMacDeviceId: StateFlow<String?>
        get() = EmptyTrustedDeviceRepositoryBindings.currentTrustedMacDeviceId
    override val previousTrustedMacDeviceId: StateFlow<String?>
        get() = EmptyTrustedDeviceRepositoryBindings.previousTrustedMacDeviceId
    override val relayMacDeviceId: StateFlow<String?>
        get() = EmptyTrustedDeviceRepositoryBindings.relayMacDeviceId

    override suspend fun switchToTrustedDevice(deviceId: String) =
        EmptyTrustedDeviceRepositoryBindings.switchToTrustedDevice(deviceId)

    override suspend fun switchToScannedDevice(payload: CodexPairingQRPayload) =
        EmptyTrustedDeviceRepositoryBindings.switchToScannedDevice(payload)

    override suspend fun cancelDeviceSwitch() = EmptyTrustedDeviceRepositoryBindings.cancelDeviceSwitch()

    override fun setDeviceMenuVisible(
        deviceId: String,
        visible: Boolean,
    ) = EmptyTrustedDeviceRepositoryBindings.setDeviceMenuVisible(deviceId, visible)
}
