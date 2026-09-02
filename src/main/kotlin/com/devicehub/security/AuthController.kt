package com.devicehub.security

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "로그인과 현재 사용자 확인 API")
class AuthController(private val authService: AuthService) {
    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
        summary = "로그인",
        description = "username과 password로 access token을 발급합니다. 이후 요청에는 Authorization: Bearer {token} 헤더를 붙입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "로그인 성공"),
            ApiResponse(responseCode = "401", description = "username 또는 password 오류"),
            ApiResponse(responseCode = "403", description = "비활성화된 계정"),
        ],
    )
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> =
        ResponseEntity.ok()
            // 발급한 token이 브라우저나 중간 캐시에 남지 않도록 한다.
            .cacheControl(CacheControl.noStore())
            .body(authService.login(request))

    @GetMapping("/me")
    @Operation(summary = "현재 사용자", description = "token에 연결된 사용자 정보를 반환합니다.")
    fun me(@AuthenticationPrincipal principal: AuthenticatedUser): ResponseEntity<AuthenticatedUser> =
        ResponseEntity.ok(principal)
}
