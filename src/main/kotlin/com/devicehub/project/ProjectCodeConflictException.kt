package com.devicehub.project

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class ProjectCodeConflictException(code: String) : RuntimeException("Project code already exists: $code")
