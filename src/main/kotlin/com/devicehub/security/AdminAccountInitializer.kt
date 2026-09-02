package com.devicehub.security

import com.devicehub.user.User
import com.devicehub.user.UserRepository
import com.devicehub.user.UserRole
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 최초 관리자 계정을 만든다.
 *
 * admin/admin123 같은 고정 계정을 코드에 넣지 않는다.
 * DEVICEHUB_ADMIN_USERNAME과 DEVICEHUB_ADMIN_PASSWORD가 함께 설정된 경우에만 생성하며,
 * 이미 ROLE_ADMIN 사용자가 있으면 아무것도 하지 않는다. 비밀번호는 로그로 남기지 않는다.
 */
@Component
class AdminAccountInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${devicehub.security.admin-username:}") private val adminUsername: String,
    @Value("\${devicehub.security.admin-password:}") private val adminPassword: String,
    @Value("\${devicehub.security.admin-name:관리자}") private val adminName: String,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(AdminAccountInitializer::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (userRepository.existsByRole(UserRole.ROLE_ADMIN)) return

        val username = adminUsername.trim()
        if (username.isEmpty() || adminPassword.isEmpty()) {
            logger.warn(
                "관리자 계정이 없습니다. DEVICEHUB_ADMIN_USERNAME과 DEVICEHUB_ADMIN_PASSWORD를 설정하고 다시 시작하면 최초 관리자를 생성한다.",
            )
            return
        }
        if (adminPassword.length < MIN_PASSWORD_LENGTH) {
            logger.warn("DEVICEHUB_ADMIN_PASSWORD가 너무 짧아 관리자 계정을 만들지 않았습니다. 최소 {}자 이상이 필요하다.", MIN_PASSWORD_LENGTH)
            return
        }
        if (userRepository.existsByUsername(username)) {
            logger.warn("이미 같은 username이 있어 관리자 계정을 만들지 않았습니다: {}", username)
            return
        }
        userRepository.save(
            User(
                username = username,
                password = passwordEncoder.encode(adminPassword),
                name = adminName,
                role = UserRole.ROLE_ADMIN,
            ),
        )
        logger.info("최초 관리자 계정을 생성했습니다: username={}", username)
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }
}
