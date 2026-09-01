package com.devicehub.device

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class DeviceDeploymentCreateRequest(
    @field:NotBlank val hospitalName: String,
    @field:NotNull val deploymentType: DeploymentType?,
    @field:NotNull val deployedAt: LocalDateTime?,
    val note: String?,
)

data class DeviceDeploymentResponse(
    val id: Long,
    val deviceId: Long,
    val hospitalName: String,
    val deploymentType: DeploymentType,
    val deployedAt: LocalDateTime,
    val returnedAt: LocalDateTime?,
    val note: String?,
    val createdAt: LocalDateTime,
)

data class DeviceDeploymentSummaryResponse(
    val id: Long,
    val hospitalName: String,
    val deploymentType: DeploymentType,
    val deployedAt: LocalDateTime,
)

@Schema(description = "현재 병원 배치 상태")
data class CurrentDeviceDeploymentResponse(
    val deployed: Boolean,
    val deployment: DeviceDeploymentResponse?,
)
