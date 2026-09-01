package com.devicehub.device

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DeviceDeploymentService(
    private val deviceRepository: DeviceRepository,
    private val deviceDeploymentRepository: DeviceDeploymentRepository,
) {
    @Transactional
    fun deploy(deviceId: Long, request: DeviceDeploymentCreateRequest): DeviceDeploymentResponse {
        if (deviceDeploymentRepository.findFirstByDeviceIdAndReturnedAtIsNullOrderByDeployedAtDesc(deviceId) != null) {
            throw DeviceDeploymentConflictException(deviceId)
        }
        val deployment = DeviceDeployment(
            device = findDevice(deviceId),
            hospitalName = request.hospitalName.trim(),
            deploymentType = requireNotNull(request.deploymentType),
            deployedAt = requireNotNull(request.deployedAt),
            note = request.note?.trim()?.takeIf(String::isNotEmpty),
        )
        return try {
            deviceDeploymentRepository.saveAndFlush(deployment).toResponse()
        } catch (exception: DataIntegrityViolationException) {
            throw DeviceDeploymentConflictException(deviceId)
        }
    }

    @Transactional
    fun returnDevice(deviceId: Long): DeviceDeploymentResponse {
        findDevice(deviceId)
        val deployment =
            deviceDeploymentRepository.findFirstByDeviceIdAndReturnedAtIsNullOrderByDeployedAtDesc(deviceId)
                ?: throw DeviceDeploymentNotFoundException(deviceId)
        deployment.returnedAt = LocalDateTime.now()
        return deviceDeploymentRepository.saveAndFlush(deployment).toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(deviceId: Long): List<DeviceDeploymentResponse> {
        findDevice(deviceId)
        return deviceDeploymentRepository.findAllByDeviceIdOrderByDeployedAtDesc(deviceId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findCurrent(deviceId: Long): CurrentDeviceDeploymentResponse {
        findDevice(deviceId)
        val current =
            deviceDeploymentRepository.findFirstByDeviceIdAndReturnedAtIsNullOrderByDeployedAtDesc(deviceId)
        return CurrentDeviceDeploymentResponse(deployed = current != null, deployment = current?.toResponse())
    }

    private fun findDevice(deviceId: Long): Device =
        deviceRepository.findById(deviceId).orElseThrow { DeviceNotFoundException(deviceId) }

    private fun DeviceDeployment.toResponse() = DeviceDeploymentResponse(
        id = requireNotNull(id),
        deviceId = requireNotNull(device.id),
        hospitalName = hospitalName,
        deploymentType = deploymentType,
        deployedAt = deployedAt,
        returnedAt = returnedAt,
        note = note,
        createdAt = createdAt,
    )
}
