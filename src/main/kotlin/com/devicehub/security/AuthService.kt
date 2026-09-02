package com.devicehub.security

import com.devicehub.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): LoginResponse {
        val username = request.username.trim()
        val user = userRepository.findByUsername(username)
        // 비밀번호와 token은 어떤 경우에도 로그로 남기지 않는다.
        if (user == null || !passwordEncoder.matches(request.password, user.password)) {
            logger.info("로그인 실패: username={}", username)
            throw AuthenticationFailedException()
        }
        if (!user.enabled) {
            logger.info("비활성 계정 로그인 시도: username={}", username)
            throw AccountDisabledException()
        }
        val userId = requireNotNull(user.id)
        logger.info("로그인 성공: username={}, role={}", username, user.role)
        return LoginResponse(
            accessToken = jwtTokenProvider.createToken(userId, user.username, user.role),
            tokenType = "Bearer",
            expiresIn = jwtTokenProvider.expirationSeconds,
            user = LoginUserResponse(userId, user.username, user.name, user.role),
        )
    }
}
