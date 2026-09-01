package com.devicehub.device

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/devices/{deviceId}/projects")
@Tag(name = "Device Project", description = "기기별 프로젝트와 설치 버전 관리 API")
class DeviceProjectController(
    private val deviceProjectService: DeviceProjectService,
) {
    @PostMapping
    @Operation(summary = "프로젝트 등록", description = "기기에 설치된 프로젝트와 현재·최신 버전을 등록합니다.")
    fun create(
        @PathVariable deviceId: Long,
        @Valid @RequestBody request: DeviceProjectCreateRequest,
    ): ResponseEntity<DeviceProjectResponse> {
        val response = deviceProjectService.create(deviceId, request)
        return ResponseEntity.created(URI.create("/api/devices/$deviceId/projects/" + response.id)).body(response)
    }

    @GetMapping
    @Operation(summary = "프로젝트 목록 조회")
    fun findAll(@PathVariable deviceId: Long): ResponseEntity<List<DeviceProjectResponse>> =
        ResponseEntity.ok(deviceProjectService.findAll(deviceId))

    @GetMapping("/{projectId}")
    @Operation(summary = "프로젝트 상세 조회")
    fun findById(
        @PathVariable deviceId: Long,
        @PathVariable projectId: Long,
    ): ResponseEntity<DeviceProjectResponse> =
        ResponseEntity.ok(deviceProjectService.findById(deviceId, projectId))

    @PutMapping("/{projectId}")
    @Operation(summary = "프로젝트 수정", description = "설치 버전, 최신 버전과 마지막 업데이트 시각을 수정합니다.")
    fun update(
        @PathVariable deviceId: Long,
        @PathVariable projectId: Long,
        @Valid @RequestBody request: DeviceProjectUpdateRequest,
    ): ResponseEntity<DeviceProjectResponse> =
        ResponseEntity.ok(deviceProjectService.update(deviceId, projectId, request))

    @DeleteMapping("/{projectId}")
    @Operation(summary = "프로젝트 삭제")
    @ApiResponse(responseCode = "204", description = "프로젝트 삭제 성공")
    fun delete(
        @PathVariable deviceId: Long,
        @PathVariable projectId: Long,
    ): ResponseEntity<Void> {
        deviceProjectService.delete(deviceId, projectId)
        return ResponseEntity.noContent().build()
    }
}
