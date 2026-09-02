package com.devicehub.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "관리자 전용 사용자 관리 API")
class UserController(private val userService: UserService) {
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "사용자 생성", description = "ROLE_ADMIN만 호출할 수 있습니다. 비밀번호는 BCrypt로 해시해 저장합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공"),
            ApiResponse(responseCode = "401", description = "인증 필요"),
            ApiResponse(responseCode = "403", description = "ROLE_ADMIN 권한 필요"),
            ApiResponse(responseCode = "409", description = "이미 존재하는 username"),
        ],
    )
    fun create(@Valid @RequestBody request: UserCreateRequest): ResponseEntity<UserResponse> {
        val response = userService.create(request)
        return ResponseEntity.created(URI.create("/api/users/" + response.id)).body(response)
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "사용자 목록", description = "ROLE_ADMIN만 호출할 수 있습니다. 비밀번호는 포함하지 않습니다.")
    fun findAll(): ResponseEntity<List<UserResponse>> = ResponseEntity.ok(userService.findAll())
}
