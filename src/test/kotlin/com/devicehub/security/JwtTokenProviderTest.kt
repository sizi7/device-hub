package com.devicehub.security

import com.devicehub.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

/**
 * JWT 발급과 검증 단위 테스트. 실제 운영 secret은 사용하지 않는다.
 */
class JwtTokenProviderTest {
    // 테스트 전용 32byte 이상 문자열이다. 운영 JWT_SECRET과 무관하다.
    private val testSecret = "jwt-token-provider-unit-test-secret-value"

    private fun provider(expirationSeconds: Long = 3600): JwtTokenProvider =
        JwtTokenProvider(
            JwtProperties(jwtSecret = testSecret, jwtExpirationSeconds = expirationSeconds),
            MockEnvironment(),
        )

    @Test
    fun `발급한 token에서 사용자 정보를 다시 꺼낼 수 있다`() {
        val token = provider().createToken(7L, "release-manager", UserRole.ROLE_RELEASE_MANAGER)
        val payload = provider().parseToken(token)
        assertNotNull(payload)
        assertEquals(7L, payload!!.userId)
        assertEquals("release-manager", payload.username)
        assertEquals(UserRole.ROLE_RELEASE_MANAGER, payload.role)
    }

    @Test
    fun `서명이 다른 token은 검증에 실패한다`() {
        val token = provider().createToken(1L, "admin", UserRole.ROLE_ADMIN)
        val otherProvider = JwtTokenProvider(
            JwtProperties(jwtSecret = "completely-different-unit-test-secret-value", jwtExpirationSeconds = 3600),
            MockEnvironment(),
        )
        assertNull(otherProvider.parseToken(token))
    }

    @Test
    fun `만료된 token은 검증에 실패한다`() {
        // 유효 시간을 음수로 두면 발급 즉시 만료된 token이 된다.
        val expiredToken = provider(expirationSeconds = -60).createToken(1L, "admin", UserRole.ROLE_ADMIN)
        assertNull(provider().parseToken(expiredToken))
    }

    @Test
    fun `형식이 아닌 문자열은 검증에 실패한다`() {
        assertNull(provider().parseToken("not-a-jwt"))
    }

    @Test
    fun `짧은 secret은 기동 시점에 거부한다`() {
        val exception = runCatching {
            JwtTokenProvider(JwtProperties(jwtSecret = "short-secret", jwtExpirationSeconds = 3600), MockEnvironment())
        }.exceptionOrNull()
        assertNotNull(exception)
    }

    @Test
    fun `prod 프로파일에서 secret이 없으면 기동을 실패시킨다`() {
        val environment = MockEnvironment().apply { setActiveProfiles("prod") }
        val exception = runCatching {
            JwtTokenProvider(JwtProperties(jwtSecret = "", jwtExpirationSeconds = 3600), environment)
        }.exceptionOrNull()
        assertNotNull(exception)
    }
}
