package com.devicehub.security

import com.devicehub.user.UserRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "로그인 요청")
data class LoginRequest(
    @field:NotBlank @field:Schema(description = "로그인 ID", example = "admin") val username: String,
    @field:NotBlank @field:Schema(description = "비밀번호") val password: String,
)

@Schema(description = "로그인한 사용자 정보")
data class LoginUserResponse(
    val id: Long,
    val username: String,
    val name: String,
    val role: UserRole,
)

@Schema(description = "로그인 응답. accessToken을 Authorization: Bearer {token} 헤더로 보냅니다.")
data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    @Schema(description = "access token 유효 시간(초)", example = "3600") val expiresIn: Long,
    val user: LoginUserResponse,
)
