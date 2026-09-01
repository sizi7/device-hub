package com.devicehub.device

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
@RequestMapping("/api/devices")
@Tag(name = "Devices", description = "Device를 등록하고 조회·수정·삭제하는 관리 API")
class DeviceController(
    private val deviceService: DeviceService,
) {
    @PostMapping
    @Operation(
        summary = "Device 등록",
        description = "기기 정보 하나를 PostgreSQL에 저장합니다. name, type, manufacturer, modelName은 필수입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Device 등록 성공"),
            ApiResponse(responseCode = "400", description = "필수 값 누락 또는 잘못된 요청"),
        ],
    )
    fun create(@Valid @RequestBody request: DeviceCreateRequest): ResponseEntity<DeviceResponse> {
        val response = deviceService.create(request)
        return ResponseEntity.created(URI.create("/api/devices/${response.id}")).body(response)
    }

    @GetMapping
    @Operation(
        summary = "Device 목록 조회",
        description = "현재 저장된 모든 Device를 목록으로 조회합니다. 데이터가 없으면 빈 배열을 반환합니다.",
    )
    @ApiResponse(responseCode = "200", description = "Device 목록 조회 성공")
    fun findAll(): ResponseEntity<List<DeviceResponse>> =
        ResponseEntity.ok(deviceService.findAll())

    @GetMapping("/connected")
    @Operation(
        summary = "ADB 연결 기기 감지",
        description = "Spring Boot 서버 PC에 ADB로 연결된 Android 기기를 감지하고 연결 없음, USB 디버깅 미승인, offline, 다중 연결, 중복 등록 상태를 구분합니다.",
    )
    @ApiResponse(responseCode = "200", description = "ADB 감지 결과 반환")
    fun findConnected(): ResponseEntity<ConnectedDeviceResponse> =
        ResponseEntity.ok(deviceService.findConnected())

    @GetMapping("/{id}")
    @Operation(
        summary = "Device 상세 조회",
        description = "ID에 해당하는 Device 한 건을 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Device 조회 성공"),
            ApiResponse(responseCode = "404", description = "해당 ID의 Device가 없음"),
        ],
    )
    fun findById(
        @Parameter(description = "조회할 Device의 ID", example = "1")
        @PathVariable id: Long,
    ): ResponseEntity<DeviceResponse> =
        ResponseEntity.ok(deviceService.findById(id))

    @PutMapping("/{id}")
    @Operation(
        summary = "Device 수정",
        description = "ID에 해당하는 Device 정보를 요청 본문의 값으로 수정합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Device 수정 성공"),
            ApiResponse(responseCode = "400", description = "필수 값 누락 또는 잘못된 요청"),
            ApiResponse(responseCode = "404", description = "해당 ID의 Device가 없음"),
        ],
    )
    fun update(
        @Parameter(description = "수정할 Device의 ID", example = "1")
        @PathVariable id: Long,
        @Valid @RequestBody request: DeviceUpdateRequest,
    ): ResponseEntity<DeviceResponse> = ResponseEntity.ok(deviceService.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Device 삭제",
        description = "ID에 해당하는 Device를 PostgreSQL에서 삭제합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Device 삭제 성공, 응답 본문 없음"),
            ApiResponse(responseCode = "404", description = "해당 ID의 Device가 없음"),
        ],
    )
    fun delete(
        @Parameter(description = "삭제할 Device의 ID", example = "1")
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        deviceService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
