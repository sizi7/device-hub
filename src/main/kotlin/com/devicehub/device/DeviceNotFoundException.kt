package com.devicehub.device

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class DeviceNotFoundException(id: Long) : RuntimeException("Device not found: $id")
