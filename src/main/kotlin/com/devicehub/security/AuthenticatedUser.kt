package com.devicehub.security

import com.devicehub.user.UserRole

/**
 * SecurityContext에 담기는 인증 주체.
 *
 * 감사 로그와 컨트롤러에서 "지금 누가 요청했는지"를 알기 위해 필요한 최소 정보만 담는다.
 * 비밀번호 해시는 담지 않는다.
 */
data class AuthenticatedUser(val userId: Long, val username: String, val role: UserRole)
