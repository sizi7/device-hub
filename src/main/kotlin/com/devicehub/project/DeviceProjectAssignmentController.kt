package com.devicehub.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/devices/{deviceId}/project-assignments")
@Tag(name = "Device Project Assignments", description = "Device의 현재 프로젝트와 과거 할당 이력 API")
class DeviceProjectAssignmentController(
    private val assignmentService: DeviceProjectAssignmentService,
) {
    @PostMapping
    @Operation(summary = "Device 프로젝트 할당", description = "Device에 현재 프로젝트를 할당합니다. 활성 할당은 기기당 하나만 허용합니다.")
    @ApiResponses(value = [ApiResponse(responseCode = "201", description = "할당 성공"), ApiResponse(responseCode = "404", description = "Device 또는 Project 없음"), ApiResponse(responseCode = "409", description = "이미 활성 프로젝트가 있음")])
    fun assign(
        @PathVariable deviceId: Long,
        @Valid @RequestBody request: DeviceProjectAssignmentCreateRequest,
    ): ResponseEntity<DeviceProjectAssignmentResponse> {
        val response = assignmentService.assign(deviceId, request)
        return ResponseEntity.created(URI.create("/api/devices/$deviceId/project-assignments/" + response.id)).body(response)
    }
    @GetMapping
    @Operation(summary = "Device 프로젝트 할당 이력", description = "종료된 과거 할당을 포함해 최신 할당순으로 반환합니다.")
    fun findAll(@PathVariable deviceId: Long) = ResponseEntity.ok(assignmentService.findAll(deviceId))
    @GetMapping("/current")
    @Operation(summary = "Device 현재 프로젝트", description = "endedAt이 없는 현재 할당과 설치·최신 APK 버전 상태를 반환합니다.")
    fun findCurrent(@PathVariable deviceId: Long) = ResponseEntity.ok(assignmentService.findCurrent(deviceId))
    @PostMapping("/end")
    @Operation(summary = "Device 프로젝트 할당 종료", description = "활성 할당에 endedAt을 기록하고 과거 이력으로 보존합니다.")
    fun end(@PathVariable deviceId: Long) = ResponseEntity.ok(assignmentService.end(deviceId))
}
