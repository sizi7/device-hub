package com.devicehub.device

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
) {
    @Transactional
    fun create(request: DeviceCreateRequest): DeviceResponse {
        val device = Device(
            name = request.name,
            type = requireNotNull(request.type),
            manufacturer = request.manufacturer,
            modelName = request.modelName,
            osVersion = request.osVersion,
        )

        return deviceRepository.save(device).toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<DeviceResponse> =
        deviceRepository.findAll().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun findById(id: Long): DeviceResponse = findDevice(id).toResponse()

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
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
