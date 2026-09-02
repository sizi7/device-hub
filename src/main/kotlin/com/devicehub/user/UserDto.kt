package com.devicehub.user

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "사용자 생성 요청. 비밀번호는 BCrypt 해시로만 저장하며 응답에 포함하지 않습니다.")
data class UserCreateRequest(
    @field:NotBlank @field:Size(min = 3, max = 50)
    @field:Schema(description = "로그인 ID", example = "release-manager")
    val username: String,
    @field:NotBlank @field:Size(min = 8, max = 100)
    @field:Schema(description = "비밀번호. 최소 8자입니다.")
    val password: String,
    @field:NotBlank @field:Schema(description = "표시 이름", example = "배포 담당자")
    val name: String,
    @field:NotNull @field:Schema(description = "역할")
    val role: UserRole?,
)

@Schema(description = "사용자 응답. 비밀번호는 절대 포함하지 않습니다.")
data class UserResponse(
    val id: Long,
    val username: String,
    val name: String,
    val role: UserRole,
    val enabled: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
