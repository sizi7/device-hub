package com.devicehub.device

import org.springframework.data.jpa.repository.JpaRepository

interface DeviceDeploymentRepository : JpaRepository<DeviceDeployment, Long> {
    fun findFirstByDeviceIdAndReturnedAtIsNullOrderByDeployedAtDesc(deviceId: Long): DeviceDeployment?
    fun findAllByDeviceIdOrderByDeployedAtDesc(deviceId: Long): List<DeviceDeployment>
}
