package com.devicehub.health

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "DeviceHub 서버 상태 확인 API")
class HealthController {

    @GetMapping("/health")
    @Operation(
        summary = "서버 상태 확인",
        description = "DeviceHub 애플리케이션이 HTTP 요청에 응답할 수 있는지 확인합니다.",
    )
    @ApiResponse(responseCode = "200", description = "서버 응답 정상")
    fun health(): HealthResponse = HealthResponse(status = "UP")
}

@Schema(description = "서버 상태 응답")
data class HealthResponse(
    @field:Schema(description = "서버 상태", example = "UP")
    val status: String,
)
