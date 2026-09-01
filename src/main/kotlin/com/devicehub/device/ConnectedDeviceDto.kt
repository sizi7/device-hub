package com.devicehub.device

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ADB 연결 상태")
enum class ConnectedDeviceStatus {
    CONNECTED,
    NOT_FOUND,
    UNAUTHORIZED,
    OFFLINE,
    MULTIPLE,
    ALREADY_REGISTERED,
    ADB_NOT_AVAILABLE,
    ERROR,
}

@Schema(description = "ADB에서 읽은 Android 기기 정보")
data class ConnectedAndroidDevice(
    val serialNumber: String,
    val manufacturer: String,
    val modelName: String,
    val productName: String?,
    val deviceName: String?,
    val osVersion: String,
    val sdkVersion: String,
    val type: DeviceType?,
    val registeredDevice: DeviceResponse? = null,
)

@Schema(description = "ADB 연결 기기 감지 결과")
data class ConnectedDeviceResponse(
    val status: ConnectedDeviceStatus,
    val device: ConnectedAndroidDevice? = null,
    val devices: List<ConnectedAndroidDevice> = emptyList(),
    val registeredDevice: DeviceResponse? = null,
    val message: String? = null,
)
