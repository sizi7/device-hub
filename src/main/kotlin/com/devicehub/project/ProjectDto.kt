package com.devicehub.project

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.time.LocalDateTime

@Schema(description = "프로젝트 등록 요청")
data class ProjectCreateRequest(
    @field:NotBlank @field:Schema(description = "프로젝트명", example = "thynC Physician") val name: String,
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z0-9_]+$")
    @field:Schema(description = "중복되지 않는 프로젝트 식별 코드", example = "THYNC_PHYSICIAN")
    val code: String,
    @field:Schema(description = "프로젝트 설명", nullable = true) val description: String?,
    @field:Schema(description = "프로젝트 관리자 또는 담당 조직", example = "Device Team", nullable = true) val manager: String?,
    @field:NotNull @field:Schema(description = "프로젝트 진행 상태", example = "OPERATING") val status: ProjectStatus?,
)

@Schema(description = "프로젝트 수정 요청")
data class ProjectUpdateRequest(
    @field:NotBlank @field:Schema(description = "프로젝트명", example = "thynC Physician") val name: String,
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z0-9_]+$")
    @field:Schema(description = "중복되지 않는 프로젝트 식별 코드", example = "THYNC_PHYSICIAN")
    val code: String,
    @field:Schema(description = "프로젝트 설명", nullable = true) val description: String?,
    @field:Schema(description = "프로젝트 관리자 또는 담당 조직", nullable = true) val manager: String?,
    @field:NotNull @field:Schema(description = "프로젝트 진행 상태") val status: ProjectStatus?,
)

@Schema(description = "프로젝트의 최근 업로드 APK 요약")
data class ProjectApkSummaryResponse(
    val id: Long,
    val version: String,
    val versionCode: Long,
    val environmentType: ProjectEnvironmentType,
    val uploadedAt: LocalDateTime,
)

@Schema(description = "프로젝트 응답")
data class ProjectResponse(
    val id: Long,
    val name: String,
    val code: String,
    val description: String?,
    val manager: String?,
    val status: ProjectStatus,
    val connectedDeviceCount: Long,
    val latestApk: ProjectApkSummaryResponse?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
