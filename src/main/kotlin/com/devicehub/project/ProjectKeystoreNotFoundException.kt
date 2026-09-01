package com.devicehub.project

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class ProjectKeystoreNotFoundException(projectId: Long, keystoreId: Long) :
    RuntimeException("Project keystore not found: project=$projectId, keystore=$keystoreId")
