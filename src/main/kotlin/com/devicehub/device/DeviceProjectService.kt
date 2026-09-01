package com.devicehub.device

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeviceProjectService(
    private val deviceRepository: DeviceRepository,
    private val deviceProjectRepository: DeviceProjectRepository,
) {
    @Transactional
    fun create(deviceId: Long, request: DeviceProjectCreateRequest): DeviceProjectResponse {
        val project = DeviceProject(
            device = findDevice(deviceId),
            projectName = request.projectName.trim(),
            packageName = request.packageName.normalized(),
            installedVersion = request.installedVersion.normalized(),
            latestVersion = request.latestVersion.normalized(),
            lastUpdatedAt = request.lastUpdatedAt,
        )
        return deviceProjectRepository.save(project).toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(deviceId: Long): List<DeviceProjectResponse> {
        findDevice(deviceId)
        return deviceProjectRepository.findAllByDeviceIdOrderByProjectNameAsc(deviceId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findById(deviceId: Long, projectId: Long): DeviceProjectResponse =
        findProject(deviceId, projectId).toResponse()

    @Transactional
    fun update(
        deviceId: Long,
        projectId: Long,
        request: DeviceProjectUpdateRequest,
    ): DeviceProjectResponse {
        val project = findProject(deviceId, projectId)
        project.projectName = request.projectName.trim()
        project.packageName = request.packageName.normalized()
        project.installedVersion = request.installedVersion.normalized()
        project.latestVersion = request.latestVersion.normalized()
        project.lastUpdatedAt = request.lastUpdatedAt
        return deviceProjectRepository.saveAndFlush(project).toResponse()
    }

    @Transactional
    fun delete(deviceId: Long, projectId: Long) {
        deviceProjectRepository.delete(findProject(deviceId, projectId))
    }

    private fun findDevice(deviceId: Long): Device =
        deviceRepository.findById(deviceId).orElseThrow { DeviceNotFoundException(deviceId) }

    private fun findProject(deviceId: Long, projectId: Long): DeviceProject =
        deviceProjectRepository.findByIdAndDeviceId(projectId, deviceId)
            ?: throw DeviceProjectNotFoundException(deviceId, projectId)

    private fun DeviceProject.toResponse() = DeviceProjectResponse(
        id = requireNotNull(id),
        deviceId = requireNotNull(device.id),
        projectName = projectName,
        packageName = packageName,
        installedVersion = installedVersion,
        latestVersion = latestVersion,
        lastUpdatedAt = lastUpdatedAt,
        versionStatus = calculateVersionStatus(installedVersion, latestVersion),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun calculateVersionStatus(installedVersion: String?, latestVersion: String?): VersionStatus =
        when {
            installedVersion == null || latestVersion == null -> VersionStatus.UNKNOWN
            installedVersion == latestVersion -> VersionStatus.LATEST
            else -> VersionStatus.UPDATE_REQUIRED
        }

    private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}
