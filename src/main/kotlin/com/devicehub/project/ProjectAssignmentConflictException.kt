package com.devicehub.project

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class ProjectAssignmentConflictException(deviceId: Long) :
    RuntimeException("Device already has an active project assignment: $deviceId")
