package com.devicehub.project

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class ProjectNetworkNotFoundException(projectId: Long, networkId: Long) :
    RuntimeException("Project network not found: project=$projectId, network=$networkId")
