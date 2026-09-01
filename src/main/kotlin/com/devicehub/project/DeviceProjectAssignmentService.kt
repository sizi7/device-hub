package com.devicehub.project

import com.devicehub.device.Device
import com.devicehub.device.DeviceDeploymentRepository
import com.devicehub.device.DeviceNotFoundException
import com.devicehub.device.DeviceProjectRepository
import com.devicehub.device.VersionStatus
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DeviceProjectAssignmentService(
    private val deviceRepository: com.devicehub.device.DeviceRepository,
    private val projectRepository: ProjectRepository,
    private val assignmentRepository: DeviceProjectAssignmentRepository,
    private val deviceProjectRepository: DeviceProjectRepository,
    private val projectApkRepository: ProjectApkRepository,
    private val deviceDeploymentRepository: DeviceDeploymentRepository,
) {
    @Transactional
    fun assign(deviceId: Long, request: DeviceProjectAssignmentCreateRequest): DeviceProjectAssignmentResponse {
        if (assignmentRepository.findFirstByDeviceIdAndEndedAtIsNullOrderByAssignedAtDesc(deviceId) != null) {
            throw ProjectAssignmentConflictException(deviceId)
        }
        val assignment = DeviceProjectAssignment(
            device = findDevice(deviceId),
            project = findProject(requireNotNull(request.projectId)),
            assignedAt = requireNotNull(request.assignedAt),
            note = request.note.normalized(),
        )
        return try {
            assignmentRepository.saveAndFlush(assignment).toResponse()
        } catch (exception: DataIntegrityViolationException) {
            throw ProjectAssignmentConflictException(deviceId)
        }
    }

    @Transactional(readOnly = true)
    fun findAll(deviceId: Long): List<DeviceProjectAssignmentResponse> {
        findDevice(deviceId)
        return assignmentRepository.findAllByDeviceIdOrderByAssignedAtDesc(deviceId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findCurrent(deviceId: Long): CurrentProjectAssignmentResponse {
        findDevice(deviceId)
        val current = assignmentRepository.findFirstByDeviceIdAndEndedAtIsNullOrderByAssignedAtDesc(deviceId)
        return CurrentProjectAssignmentResponse(current != null, current?.toResponse())
    }

    @Transactional
    fun end(deviceId: Long): DeviceProjectAssignmentResponse {
        findDevice(deviceId)
        val assignment = assignmentRepository.findFirstByDeviceIdAndEndedAtIsNullOrderByAssignedAtDesc(deviceId)
            ?: throw ProjectAssignmentNotFoundException(deviceId)
        assignment.endedAt = LocalDateTime.now()
        return assignmentRepository.saveAndFlush(assignment).toResponse()
    }

    @Transactional(readOnly = true)
    fun findProjectDevices(projectId: Long): List<AssignedDeviceResponse> {
        findProject(projectId)
        return assignmentRepository.findAllByProjectIdAndEndedAtIsNullOrderByAssignedAtDesc(projectId)
            .map { assignment ->
                val versions = versions(assignment.device, assignment.project)
                val deployment = deviceDeploymentRepository
                    .findFirstByDeviceIdAndReturnedAtIsNullOrderByDeployedAtDesc(requireNotNull(assignment.device.id))
                val location = if (deployment == null) {
                    "사내"
                } else {
                    deployment.hospitalName + if (deployment.deploymentType.name == "HOSPITAL_LOAN") " 대여" else " 전용"
                }
                AssignedDeviceResponse(
                    assignmentId = requireNotNull(assignment.id),
                    deviceId = requireNotNull(assignment.device.id),
                    name = assignment.device.name,
                    modelName = assignment.device.modelName,
                    manufacturer = assignment.device.manufacturer,
                    currentLocation = location,
                    installedVersion = versions.installed,
                    latestVersion = versions.latest,
                    versionStatus = versions.status,
                    assignedAt = assignment.assignedAt,
                )
            }
    }

    private fun DeviceProjectAssignment.toResponse(): DeviceProjectAssignmentResponse {
        val versions = versions(device, project)
        return DeviceProjectAssignmentResponse(
            id = requireNotNull(id),
            deviceId = requireNotNull(device.id),
            project = AssignedProjectSummaryResponse(
                id = requireNotNull(project.id),
                name = project.name,
                code = project.code,
                status = project.status,
            ),
            assignedAt = assignedAt,
            endedAt = endedAt,
            note = note,
            installedVersion = versions.installed,
            latestVersion = versions.latest,
            versionStatus = versions.status,
            createdAt = createdAt,
        )
    }

    private fun versions(device: Device, project: Project): VersionValues {
        val installed = deviceProjectRepository
            .findFirstByDeviceIdAndProjectNameIgnoreCase(requireNotNull(device.id), project.name)
            ?.installedVersion
        val latest = projectApkRepository.findFirstByProjectIdOrderByUploadedAtDesc(requireNotNull(project.id))?.version
        val status = when {
            installed == null || latest == null -> VersionStatus.UNKNOWN
            installed == latest -> VersionStatus.LATEST
            else -> VersionStatus.UPDATE_REQUIRED
        }
        return VersionValues(installed, latest, status)
    }

    private fun findDevice(id: Long): Device =
        deviceRepository.findById(id).orElseThrow { DeviceNotFoundException(id) }
    private fun findProject(id: Long): Project =
        projectRepository.findById(id).orElseThrow { ProjectNotFoundException(id) }
    private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)
    private data class VersionValues(val installed: String?, val latest: String?, val status: VersionStatus)
}
