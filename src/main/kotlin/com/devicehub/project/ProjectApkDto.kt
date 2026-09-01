package com.devicehub.project

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "프로젝트 APK 메타데이터 응답. 파일 binary와 서버 내부 경로는 노출하지 않습니다.")
data class ProjectApkResponse(
    val id: Long,
    val projectId: Long,
    val projectName: String,
    val projectCode: String,
    val version: String,
    val versionCode: Long,
    val fileName: String,
    val environmentType: ProjectEnvironmentType,
    val releaseNote: String?,
    val uploadedAt: LocalDateTime,
    val createdAt: LocalDateTime,
)

@Schema(description = "프로젝트 환경별 최신 APK 응답")
data class LatestProjectApkResponse(
    val projectId: Long,
    val projectName: String,
    val environmentType: ProjectEnvironmentType,
    val version: String,
    val versionCode: Long,
    val uploadedAt: LocalDateTime,
)
