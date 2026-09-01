package com.devicehub.project

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Schema(description = "프로젝트 키스토어 메타데이터 수정 요청")
data class ProjectKeystoreUpdateRequest(
    @field:NotBlank @field:Schema(description = "키스토어 설정 이름", example = "THYNC Release Keystore") val name: String,
    @field:Schema(description = "키스토어 설명", nullable = true) val description: String?,
)

@Schema(description = "프로젝트 키스토어 비밀번호 변경 요청. 저장된 키스토어 파일로 실제 검증한 뒤 반영합니다.")
data class ProjectKeystorePasswordRequest(
    @field:NotBlank @field:Schema(description = "키 alias", example = "release") val keyAlias: String,
    @field:NotBlank @field:Schema(description = "스토어 비밀번호") val storePassword: String,
    @field:Schema(description = "키 비밀번호. 비우면 스토어 비밀번호와 같다고 봅니다.", nullable = true) val keyPassword: String?,
)

@Schema(description = "프로젝트 키스토어 응답. 비밀번호는 절대 포함하지 않습니다.")
data class ProjectKeystoreResponse(
    val id: Long,
    val projectId: Long,
    val projectCode: String,
    val name: String,
    val fileName: String,
    @Schema(description = "키스토어 형식", example = "PKCS12") val storeType: String,
    val keyAlias: String,
    @Schema(description = "키 비밀번호를 스토어 비밀번호와 다르게 저장했는지 여부") val hasSeparateKeyPassword: Boolean,
    val description: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

@Schema(description = "복호화한 키스토어 비밀번호 응답. 노출에 주의해야 하는 값입니다.")
data class ProjectKeystorePasswordResponse(
    val id: Long,
    val keyAlias: String,
    val storePassword: String,
    val keyPassword: String,
)
