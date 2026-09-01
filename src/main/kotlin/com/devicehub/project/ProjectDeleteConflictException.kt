package com.devicehub.project

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class ProjectDeleteConflictException(id: Long) :
    RuntimeException("Project has related assignments, networks, or APKs: $id")
