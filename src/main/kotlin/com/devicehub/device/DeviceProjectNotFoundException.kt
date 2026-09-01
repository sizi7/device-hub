package com.devicehub.device

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class DeviceProjectNotFoundException(deviceId: Long, projectId: Long) :
    RuntimeException("Device project not found: device=$deviceId, project=$projectId")
