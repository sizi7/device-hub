package com.devicehub.user

import com.devicehub.security.AuthenticatedUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
@RequestMapping("/api/users")
@Tag(name = "Users", description = "관리자 전용 사용자 관리 API")
class UserController(private val userService: UserService) {
    @PostMapping
    @PreAuthorize(ADMIN_ONLY)
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
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "사용자 목록", description = "ROLE_ADMIN만 호출할 수 있습니다. 비밀번호는 포함하지 않습니다.")
    fun findAll(): ResponseEntity<List<UserResponse>> = ResponseEntity.ok(userService.findAll())

    @GetMapping("/{id}")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "사용자 상세", description = "ROLE_ADMIN만 호출할 수 있습니다.")
    fun findById(@PathVariable id: Long): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.findById(id))

    @PutMapping("/{id}")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(
        summary = "사용자 수정",
        description = "표시 이름, 역할, 활성 여부를 수정합니다. username과 비밀번호는 바꾸지 않습니다. " +
            "활성 관리자가 0명이 되는 변경은 거부합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공"),
            ApiResponse(responseCode = "401", description = "인증 필요"),
            ApiResponse(responseCode = "403", description = "ROLE_ADMIN 권한 필요"),
            ApiResponse(responseCode = "404", description = "사용자 없음"),
            ApiResponse(responseCode = "409", description = "마지막 활성 관리자를 내릴 수 없음"),
        ],
    )
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UserUpdateRequest): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.update(id, request))

    @PutMapping("/{id}/password")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(
        summary = "비밀번호 재설정",
        description = "관리자가 다른 사용자의 비밀번호를 재설정합니다. 현재 비밀번호는 필요하지 않습니다. " +
            "응답에는 비밀번호를 포함하지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "재설정 성공"),
            ApiResponse(responseCode = "400", description = "비밀번호 규칙 위반"),
            ApiResponse(responseCode = "401", description = "인증 필요"),
            ApiResponse(responseCode = "403", description = "ROLE_ADMIN 권한 필요"),
            ApiResponse(responseCode = "404", description = "사용자 없음"),
        ],
    )
    fun resetPassword(
        @PathVariable id: Long,
        @Valid @RequestBody request: UserPasswordResetRequest,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userService.resetPassword(id, request))

    @DeleteMapping("/{id}")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(
        summary = "사용자 삭제",
        description = "본인 계정과 마지막 활성 관리자는 삭제할 수 없습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(responseCode = "401", description = "인증 필요"),
            ApiResponse(responseCode = "403", description = "ROLE_ADMIN 권한 필요"),
            ApiResponse(responseCode = "404", description = "사용자 없음"),
            ApiResponse(responseCode = "409", description = "본인 계정 또는 마지막 활성 관리자"),
        ],
    )
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal principal: AuthenticatedUser,
    ): ResponseEntity<Void> {
        userService.delete(id, principal.userId)
        return ResponseEntity.noContent().build()
    }

    companion object {
        private const val ADMIN_ONLY = "hasAuthority('ROLE_ADMIN')"
    }
}
