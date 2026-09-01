package com.devicehub.project

import org.springframework.data.jpa.repository.JpaRepository

interface DeviceProjectAssignmentRepository : JpaRepository<DeviceProjectAssignment, Long> {
    fun findFirstByDeviceIdAndEndedAtIsNullOrderByAssignedAtDesc(deviceId: Long): DeviceProjectAssignment?
    fun findAllByDeviceIdOrderByAssignedAtDesc(deviceId: Long): List<DeviceProjectAssignment>
    fun findAllByProjectIdAndEndedAtIsNullOrderByAssignedAtDesc(projectId: Long): List<DeviceProjectAssignment>
    fun countByProjectIdAndEndedAtIsNull(projectId: Long): Long
}
