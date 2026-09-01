package com.devicehub.device

import org.springframework.data.jpa.repository.JpaRepository

interface DeviceProjectRepository : JpaRepository<DeviceProject, Long> {
    fun findAllByDeviceIdOrderByProjectNameAsc(deviceId: Long): List<DeviceProject>
    fun findByIdAndDeviceId(id: Long, deviceId: Long): DeviceProject?
    fun countByDeviceId(deviceId: Long): Long
    fun findFirstByDeviceIdAndProjectNameIgnoreCase(deviceId: Long, projectName: String): DeviceProject?
}
