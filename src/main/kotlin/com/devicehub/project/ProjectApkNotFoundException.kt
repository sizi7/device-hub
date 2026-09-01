package com.devicehub.project

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class ProjectApkNotFoundException(projectId: Long, apkId: Long) :
    RuntimeException("Project APK not found: project=$projectId, apk=$apkId")
