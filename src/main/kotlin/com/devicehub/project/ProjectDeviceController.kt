package com.devicehub.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/projects/{projectId}/devices")
@Tag(name = "Device Project Assignments", description = "프로젝트에 현재 연결된 Device 조회 API")
class ProjectDeviceController(private val assignmentService: DeviceProjectAssignmentService) {
    @GetMapping
    @Operation(summary = "프로젝트 연결 Device 목록", description = "현재 프로젝트에 할당된 Device와 위치, 설치 버전, 최신 APK 버전 및 업데이트 상태를 반환합니다.")
    fun findAll(@PathVariable projectId: Long): ResponseEntity<List<AssignedDeviceResponse>> =
        ResponseEntity.ok(assignmentService.findProjectDevices(projectId))
}
