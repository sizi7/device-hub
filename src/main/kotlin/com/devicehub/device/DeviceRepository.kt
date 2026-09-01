package com.devicehub.device

import org.springframework.data.jpa.repository.JpaRepository

interface DeviceRepository : JpaRepository<Device, Long> {
    fun findBySerialNumber(serialNumber: String): Device?
}
