package com.devicehub.project

import com.devicehub.device.VersionStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Schema(description = "Device 프로젝트 할당 요청")
data class DeviceProjectAssignmentCreateRequest(
    @field:NotNull @field:Schema(description = "할당할 Project ID", example = "1") val projectId: Long?,
    @field:NotNull @field:Schema(description = "할당 시작 시각", example = "2026-09-01T15:30:00") val assignedAt: LocalDateTime?,
    @field:Schema(description = "할당 메모", nullable = true) val note: String?,
)

data class AssignedProjectSummaryResponse(
    val id: Long,
    val name: String,
    val code: String,
    val status: ProjectStatus,
)

@Schema(description = "Device 프로젝트 할당 이력 응답. endedAt이 null이면 현재 할당입니다.")
data class DeviceProjectAssignmentResponse(
    val id: Long,
    val deviceId: Long,
    val project: AssignedProjectSummaryResponse,
    val assignedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val note: String?,
    val installedVersion: String?,
    val latestVersion: String?,
    val versionStatus: VersionStatus,
    val createdAt: LocalDateTime,
)

@Schema(description = "Device 현재 프로젝트 할당 상태")
data class CurrentProjectAssignmentResponse(
    val assigned: Boolean,
    val assignment: DeviceProjectAssignmentResponse?,
)

@Schema(description = "프로젝트에 현재 할당된 Device와 버전 상태")
data class AssignedDeviceResponse(
    val assignmentId: Long,
    val deviceId: Long,
    val name: String,
    val modelName: String,
    val manufacturer: String,
    val currentLocation: String,
    val installedVersion: String?,
    val latestVersion: String?,
    val versionStatus: VersionStatus,
    val assignedAt: LocalDateTime,
)
