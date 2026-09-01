package com.devicehub.device

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class DeviceDeploymentNotFoundException(deviceId: Long) :
    RuntimeException("Active deployment not found: $deviceId")
