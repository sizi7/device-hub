package com.devicehub.device

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
@RequestMapping("/api/devices/{deviceId}/deployments")
@Tag(name = "Device Deployment", description = "기기의 병원 배치, 회수와 이력 관리 API")
class DeviceDeploymentController(
    private val deviceDeploymentService: DeviceDeploymentService,
) {
    @PostMapping
    @Operation(summary = "병원 배치", description = "기기를 병원 임시 대여 또는 병원 전용으로 배치합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "병원 배치 성공"),
            ApiResponse(responseCode = "409", description = "이미 병원에 배치 중"),
        ],
    )
    fun deploy(
        @PathVariable deviceId: Long,
        @Valid @RequestBody request: DeviceDeploymentCreateRequest,
    ): ResponseEntity<DeviceDeploymentResponse> {
        val response = deviceDeploymentService.deploy(deviceId, request)
        return ResponseEntity.created(URI.create("/api/devices/$deviceId/deployments/" + response.id)).body(response)
    }

    @PostMapping("/return")
    @Operation(summary = "병원 회수", description = "현재 병원 배치를 종료하고 returnedAt을 기록합니다.")
    fun returnDevice(@PathVariable deviceId: Long): ResponseEntity<DeviceDeploymentResponse> =
        ResponseEntity.ok(deviceDeploymentService.returnDevice(deviceId))

    @GetMapping
    @Operation(summary = "병원 배치 이력", description = "회수된 기록을 포함한 전체 배치 이력을 최신순으로 조회합니다.")
    fun findAll(@PathVariable deviceId: Long): ResponseEntity<List<DeviceDeploymentResponse>> =
        ResponseEntity.ok(deviceDeploymentService.findAll(deviceId))

    @GetMapping("/current")
    @Operation(summary = "현재 병원 배치 상태", description = "returnedAt이 없는 현재 배치 상태를 조회합니다.")
    fun findCurrent(@PathVariable deviceId: Long): ResponseEntity<CurrentDeviceDeploymentResponse> =
        ResponseEntity.ok(deviceDeploymentService.findCurrent(deviceId))
}
