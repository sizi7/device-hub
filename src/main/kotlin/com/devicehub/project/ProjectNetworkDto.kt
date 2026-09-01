package com.devicehub.project

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Schema(description = "프로젝트 네트워크 등록·수정 요청. Secret과 Token은 포함하지 않습니다.")
data class ProjectNetworkRequest(
    @field:NotNull @field:Schema(description = "네트워크 환경", example = "ISO") val environmentType: ProjectEnvironmentType?,
    @field:NotBlank @field:Schema(description = "설정 이름", example = "ISO Production Network") val name: String,
    @field:Schema(description = "HTTP API 기본 URL", example = "https://iso-api.example.com", nullable = true) val apiUrl: String?,
    @field:Schema(description = "WebSocket 또는 TCP Socket URL", example = "wss://iso-socket.example.com", nullable = true) val socketUrl: String?,
    @field:Schema(description = "네트워크 설명", nullable = true) val description: String?,
)

@Schema(description = "프로젝트 네트워크 응답")
data class ProjectNetworkResponse(
    val id: Long,
    val projectId: Long,
    val environmentType: ProjectEnvironmentType,
    val name: String,
    val apiUrl: String?,
    val socketUrl: String?,
    val description: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
