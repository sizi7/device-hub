package com.devicehub.device

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val adbDeviceService: AdbDeviceService,
    private val deviceDeploymentRepository: DeviceDeploymentRepository,
    private val deviceProjectRepository: DeviceProjectRepository,
) {
    @Transactional
    fun create(request: DeviceCreateRequest): DeviceResponse {
        val serialNumber = request.serialNumber?.trim()?.takeIf(String::isNotEmpty)
        if (serialNumber != null && deviceRepository.findBySerialNumber(serialNumber) != null) {
            throw DuplicateSerialNumberException(serialNumber)
        }
        val device = Device(
            name = request.name,
            type = requireNotNull(request.type),
            manufacturer = request.manufacturer,
            modelName = request.modelName,
            osVersion = request.osVersion,
            serialNumber = serialNumber,
        )

        return deviceRepository.save(device).toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<DeviceResponse> =
        deviceRepository.findAll().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun findById(id: Long): DeviceResponse = findDevice(id).toResponse()

    @Transactional(readOnly = true)
    fun findConnected(): ConnectedDeviceResponse {
        val detected = adbDeviceService.findConnectedDevices()
        if (detected.status != ConnectedDeviceStatus.CONNECTED) return detected

        val devices = detected.devices.map { device ->
            val registered = deviceRepository.findBySerialNumber(device.serialNumber)?.toResponse()
            device.copy(registeredDevice = registered)
        }
        if (devices.size > 1) {
            return ConnectedDeviceResponse(status = ConnectedDeviceStatus.MULTIPLE, devices = devices)
        }

        val device = devices.single()
        return if (device.registeredDevice != null) {
            ConnectedDeviceResponse(
                status = ConnectedDeviceStatus.ALREADY_REGISTERED,
                device = device,
                registeredDevice = device.registeredDevice,
            )
        } else {
            ConnectedDeviceResponse(status = ConnectedDeviceStatus.CONNECTED, device = device)
        }
    }

    @Transactional
    fun update(id: Long, request: DeviceUpdateRequest): DeviceResponse {
        val device = findDevice(id)
        device.name = request.name
        device.type = requireNotNull(request.type)
        device.manufacturer = request.manufacturer
        device.modelName = request.modelName
        device.osVersion = request.osVersion

        return deviceRepository.saveAndFlush(device).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        deviceRepository.delete(findDevice(id))
    }

    private fun findDevice(id: Long): Device =
        deviceRepository.findById(id).orElseThrow { DeviceNotFoundException(id) }

    private fun Device.toResponse(): DeviceResponse = DeviceResponse(
        id = requireNotNull(id),
        name = name,
        type = type,
        manufacturer = manufacturer,
        modelName = modelName,
        osVersion = osVersion,
        serialNumber = serialNumber,
        currentDeployment = deviceDeploymentRepository
            .findFirstByDeviceIdAndReturnedAtIsNullOrderByDeployedAtDesc(requireNotNull(id))
            ?.toSummaryResponse(),
        projectCount = deviceProjectRepository.countByDeviceId(requireNotNull(id)),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun DeviceDeployment.toSummaryResponse(): DeviceDeploymentSummaryResponse =
        DeviceDeploymentSummaryResponse(
            id = requireNotNull(id),
            hospitalName = hospitalName,
            deploymentType = deploymentType,
            deployedAt = deployedAt,
        )
}
