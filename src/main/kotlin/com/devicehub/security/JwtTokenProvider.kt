package com.devicehub.security

import com.devicehub.user.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Date
import javax.crypto.SecretKey

/**
 * JWT access token을 발급하고 검증한다.
 *
 * 마스터 키를 소스에 하드코딩했던 SecretEncryptor의 문제를 반복하지 않기 위해
 * JWT secret에는 소스 기본값을 두지 않는다.
 *
 * - JWT_SECRET이 설정되어 있으면 그 값을 쓴다. 32byte 미만이면 기동을 실패시킨다.
 * - prod 프로파일에서 JWT_SECRET이 없으면 기동을 실패시킨다.
 * - 그 외(로컬 개발)에서 없으면 기동할 때마다 무작위 키를 새로 만든다.
 *   하드코딩 값이 남지 않는 대신 서버를 재시작하면 기존 token이 모두 무효가 된다.
 */
@Component
class JwtTokenProvider(
    properties: JwtProperties,
    environment: Environment,
) {
    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)
    val expirationSeconds: Long = properties.jwtExpirationSeconds
    private val secretKey: SecretKey = resolveKey(properties.jwtSecret, environment)

    fun createToken(userId: Long, username: String, role: UserRole): String {
        val now = Date()
        val expiration = Date(now.time + expirationSeconds * 1000)
        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_USERNAME, username)
            .claim(CLAIM_ROLE, role.name)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    /** 서명과 만료를 검증한다. 유효하지 않으면 null을 반환하고 token 내용은 로그로 남기지 않는다. */
    fun parseToken(token: String): JwtPayload? = try {
        val claims: Claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
        val role = UserRole.valueOf(claims.get(CLAIM_ROLE, String::class.java))
        JwtPayload(
            userId = claims.subject.toLong(),
            username = claims.get(CLAIM_USERNAME, String::class.java),
            role = role,
        )
    } catch (exception: JwtException) {
        null
    } catch (exception: IllegalArgumentException) {
        null
    }

    private fun resolveKey(configuredSecret: String, environment: Environment): SecretKey {
        val secret = configuredSecret.trim()
        if (secret.isNotEmpty()) {
            val bytes = secret.toByteArray(Charsets.UTF_8)
            require(bytes.size >= MIN_SECRET_BYTES) {
                "JWT_SECRET은 최소 ${MIN_SECRET_BYTES}byte 이상이어야 합니다. 더 긴 임의 문자열을 사용해 주세요."
            }
            return Keys.hmacShaKeyFor(bytes)
        }
        check(!environment.activeProfiles.contains(PROD_PROFILE)) {
            "prod 프로파일에서는 JWT_SECRET 환경변수를 반드시 설정해야 합니다."
        }
        logger.warn(
            "JWT_SECRET이 설정되지 않아 기동할 때마다 무작위 키를 생성합니다. " +
                "서버를 재시작하면 기존 로그인 token이 모두 무효가 됩니다. 운영 환경에서는 반드시 설정한다.",
        )
        val random = ByteArray(MIN_SECRET_BYTES).also(SecureRandom()::nextBytes)
        return Keys.hmacShaKeyFor(random)
    }

    companion object {
        const val CLAIM_USERNAME = "username"
        const val CLAIM_ROLE = "role"
        private const val MIN_SECRET_BYTES = 32
        private const val PROD_PROFILE = "prod"
    }
}

/** token에서 꺼낸 인증 정보. */
data class JwtPayload(val userId: Long, val username: String, val role: UserRole)
