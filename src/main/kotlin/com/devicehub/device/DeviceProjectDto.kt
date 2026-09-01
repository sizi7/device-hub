package com.devicehub.device

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class DeviceProjectCreateRequest(
    @field:NotBlank val projectName: String,
    val packageName: String?,
    val installedVersion: String?,
    val latestVersion: String?,
    val lastUpdatedAt: LocalDateTime?,
)

data class DeviceProjectUpdateRequest(
    @field:NotBlank val projectName: String,
    val packageName: String?,
    val installedVersion: String?,
    val latestVersion: String?,
    val lastUpdatedAt: LocalDateTime?,
)

data class DeviceProjectResponse(
    val id: Long,
    val deviceId: Long,
    val projectName: String,
    val packageName: String?,
    val installedVersion: String?,
    val latestVersion: String?,
    val lastUpdatedAt: LocalDateTime?,
    val versionStatus: VersionStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
