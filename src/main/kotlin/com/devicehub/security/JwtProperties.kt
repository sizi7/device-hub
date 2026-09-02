package com.devicehub.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * JWT 설정.
 *
 * secret은 환경변수 JWT_SECRET으로 주입한다. 소스에는 기본값을 두지 않는다.
 * 미설정 시 동작은 JwtTokenProvider에서 프로파일에 따라 다르게 처리한다.
 */
@ConfigurationProperties(prefix = "devicehub.security")
data class JwtProperties(
    val jwtSecret: String = "",
    /** access token 유효 시간(초). */
    val jwtExpirationSeconds: Long = 3600,
)
