package com.devicehub.device

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Schema(description = "Device 등록 요청")
data class DeviceCreateRequest(
    @field:NotBlank
    @field:Schema(description = "사용자가 구분하기 위한 기기 이름", example = "개발용 갤럭시")
    val name: String,

    @field:NotNull
    @field:Schema(description = "기기 종류", example = "PHONE")
    val type: DeviceType?,

    @field:NotBlank
    @field:Schema(description = "기기 제조사", example = "Samsung")
    val manufacturer: String,

    @field:NotBlank
    @field:Schema(description = "기기 모델명", example = "Galaxy S25+")
    val modelName: String,

    @field:Schema(description = "운영체제 버전, 모르면 생략 가능", example = "Android 16", nullable = true)
    val osVersion: String?,

    @field:Schema(description = "ADB 기기 일련번호, 수동 등록 시 생략 가능", example = "R3XXXXXXXX", nullable = true)
    val serialNumber: String? = null,
)

@Schema(description = "Device 수정 요청")
data class DeviceUpdateRequest(
    @field:NotBlank
    @field:Schema(description = "수정할 기기 이름", example = "테스트 갤럭시")
    val name: String,

    @field:NotNull
    @field:Schema(description = "수정할 기기 종류", example = "PHONE")
    val type: DeviceType?,

    @field:NotBlank
    @field:Schema(description = "수정할 기기 제조사", example = "Samsung")
    val manufacturer: String,

    @field:NotBlank
    @field:Schema(description = "수정할 기기 모델명", example = "Galaxy S25+")
    val modelName: String,

    @field:Schema(description = "수정할 운영체제 버전, 생략 가능", example = "Android 17", nullable = true)
    val osVersion: String?,
)

@Schema(description = "Device 응답")
data class DeviceResponse(
    @field:Schema(description = "자동 생성된 Device ID", example = "1")
    val id: Long,

    @field:Schema(description = "기기 이름", example = "개발용 갤럭시")
    val name: String,

    @field:Schema(description = "기기 종류", example = "PHONE")
    val type: DeviceType,

    @field:Schema(description = "기기 제조사", example = "Samsung")
    val manufacturer: String,

    @field:Schema(description = "기기 모델명", example = "Galaxy S25+")
    val modelName: String,

    @field:Schema(description = "운영체제 버전", example = "Android 16", nullable = true)
    val osVersion: String?,

    @field:Schema(description = "ADB 기기 일련번호", example = "R3XXXXXXXX", nullable = true)
    val serialNumber: String?,

    @field:Schema(description = "현재 병원 배치 요약", nullable = true)
    val currentDeployment: DeviceDeploymentSummaryResponse?,

    @field:Schema(description = "등록된 프로젝트 수", example = "2")
    val projectCount: Long,

    @field:Schema(description = "생성 시각", example = "2026-09-01T10:54:29.133456")
    val createdAt: LocalDateTime,

    @field:Schema(description = "마지막 수정 시각", example = "2026-09-01T10:57:05.658742")
    val updatedAt: LocalDateTime,
)
