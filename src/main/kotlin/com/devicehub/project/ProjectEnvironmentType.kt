package com.devicehub.project

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "프로젝트 실행 환경: ISO 검증, MFDS 인허가, 개발, 사업부")
enum class ProjectEnvironmentType {
    ISO,
    MFDS,
    DEVELOPMENT,
    BUSINESS,
}
