package com.devicehub.project

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class ProjectAssignmentNotFoundException(deviceId: Long) :
    RuntimeException("Active project assignment not found: $deviceId")
