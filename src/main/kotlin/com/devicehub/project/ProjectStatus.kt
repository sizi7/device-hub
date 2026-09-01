package com.devicehub.project

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "프로젝트 상태: 기획, 개발, 운영, 중단, 완료")
enum class ProjectStatus {
    PLANNING,
    DEVELOPMENT,
    OPERATING,
    SUSPENDED,
    COMPLETED,
}
