package com.devicehub.device

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class DeviceDeploymentConflictException(deviceId: Long) :
    RuntimeException("Device is already deployed: $deviceId")
