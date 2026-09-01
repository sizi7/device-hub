package com.devicehub.device

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateSerialNumberException(serialNumber: String) :
    RuntimeException("Device serial number already registered: $serialNumber")
