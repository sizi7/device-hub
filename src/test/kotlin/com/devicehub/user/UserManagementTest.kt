package com.devicehub.user

import com.devicehub.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.put
import org.springframework.transaction.annotation.Transactional

/**
 * 사용자 관리와 비밀번호 변경 검증.
 *
 * 테스트 전용 계정과 비밀번호만 사용하며 값을 단언 메시지나 출력에 남기지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserManagementTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val jwtTokenProvider: JwtTokenProvider,
    @Autowired private val objectMapper: ObjectMapper,
) {
    private val testPassword = "user-management-test-password"
    private val newPassword = "user-management-new-password"

    @BeforeEach
    fun setUp() {
        listOf(
            Triple("mgmt-test-admin", "관리 테스트 관리자", UserRole.ROLE_ADMIN),
            Triple("mgmt-test-admin2", "관리 테스트 관리자2", UserRole.ROLE_ADMIN),
            Triple("mgmt-test-user", "관리 테스트 일반", UserRole.ROLE_USER),
        ).forEach { (username, name, role) ->
            if (!userRepository.existsByUsername(username)) {
                userRepository.save(
                    User(
                        username = username,
                        password = passwordEncoder.encode(testPassword),
                        name = name,
                        role = role,
                    ),
                )
            }
        }
    }

    private fun user(username: String): User = requireNotNull(userRepository.findByUsername(username))

    private fun tokenFor(username: String): String {
        val target = user(username)
        return jwtTokenProvider.createToken(requireNotNull(target.id), target.username, target.role)
    }

    private fun json(value: Any): String = objectMapper.writeValueAsString(value)

    @Test
    fun `관리자는 사용자 정보를 수정할 수 있다`() {
        val target = user("mgmt-test-user")
        mockMvc.put("/api/users/" + target.id) {
            header("Authorization", "Bearer " + tokenFor("mgmt-test-admin"))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("name" to "이름 변경", "role" to "ROLE_RELEASE_MANAGER", "enabled" to true))
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("이름 변경") }
            jsonPath("$.role") { value("ROLE_RELEASE_MANAGER") }
            jsonPath("$.password") { doesNotExist() }
        }
    }

    @Test
    fun `ROLE_USER는 사용자 정보를 수정할 수 없다`() {
        val target = user("mgmt-test-user")
        mockMvc.put("/api/users/" + target.id) {
            header("Authorization", "Bearer " + tokenFor("mgmt-test-user"))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("name" to "무단 변경", "role" to "ROLE_ADMIN", "enabled" to true))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `관리자는 다른 사용자의 비밀번호를 재설정할 수 있다`() {
        val target = user("mgmt-test-user")
        mockMvc.put("/api/users/" + target.id + "/password") {
            header("Authorization", "Bearer " + tokenFor("mgmt-test-admin"))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("newPassword" to newPassword))
        }.andExpect {
            status { isOk() }
            jsonPath("$.password") { doesNotExist() }
        }
    }

    @Test
    fun `ROLE_USER는 다른 사용자의 비밀번호를 재설정할 수 없다`() {
        val target = user("mgmt-test-admin")
        mockMvc.put("/api/users/" + target.id + "/password") {
            header("Authorization", "Bearer " + tokenFor("mgmt-test-user"))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("newPassword" to newPassword))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `본인 비밀번호를 현재 비밀번호와 함께 바꿀 수 있다`() {
        mockMvc.put("/api/auth/password") {
            header("Authorization", "Bearer " + tokenFor("mgmt-test-user"))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("currentPassword" to testPassword, "newPassword" to newPassword))
        }.andExpect {
            status { isOk() }
            jsonPath("$.username") { value("mgmt-test-user") }
            jsonPath("$.password") { doesNotExist() }
        }
    }

    @Test
    fun `현재 비밀번호가 틀리면 본인 비밀번호를 바꿀 수 없다`() {
        mockMvc.put("/api/auth/password") {
            header("Authorization", "Bearer " + tokenFor("mgmt-test-user"))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("currentPassword" to "wrong-current-password", "newPassword" to newPassword))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `token 없이 본인 비밀번호를 바꿀 수 없다`() {
        mockMvc.put("/api/auth/password") {
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("currentPassword" to testPassword, "newPassword" to newPassword))
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `짧은 새 비밀번호는 거부한다`() {
        mockMvc.put("/api/auth/password") {
            header("Authorization", "Bearer " + tokenFor("mgmt-test-user"))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("currentPassword" to testPassword, "newPassword" to "short"))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `본인 계정은 삭제할 수 없다`() {
        val admin = user("mgmt-test-admin")
        mockMvc.delete("/api/users/" + admin.id) {
            header("Authorization", "Bearer " + tokenFor("mgmt-test-admin"))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `다른 활성 관리자가 있으면 관리자를 삭제할 수 있다`() {
        val target = user("mgmt-test-admin2")
        mockMvc.delete("/api/users/" + target.id) {
            header("Authorization", "Bearer " + tokenFor("mgmt-test-admin"))
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `마지막 활성 관리자는 일반 사용자로 내릴 수 없다`() {
        // 다른 관리자를 모두 비활성화해서 대상이 마지막 활성 관리자가 되도록 만든다.
        userRepository.findAllByOrderByIdAsc()
            .filter { it.role == UserRole.ROLE_ADMIN && it.username != "mgmt-test-admin" }
            .forEach { it.enabled = false }
        userRepository.flush()

        val admin = user("mgmt-test-admin")
        mockMvc.put("/api/users/" + admin.id) {
            header("Authorization", "Bearer " + tokenFor("mgmt-test-admin"))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("name" to admin.name, "role" to "ROLE_USER", "enabled" to true))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `마지막 활성 관리자는 비활성화할 수 없다`() {
        userRepository.findAllByOrderByIdAsc()
            .filter { it.role == UserRole.ROLE_ADMIN && it.username != "mgmt-test-admin" }
            .forEach { it.enabled = false }
        userRepository.flush()

        val admin = user("mgmt-test-admin")
        mockMvc.put("/api/users/" + admin.id) {
            header("Authorization", "Bearer " + tokenFor("mgmt-test-admin"))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("name" to admin.name, "role" to "ROLE_ADMIN", "enabled" to false))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `비활성화된 계정은 로그인할 수 없다`() {
        val target = user("mgmt-test-user")
        target.enabled = false
        userRepository.flush()

        mockMvc.put("/api/auth/password") {
            header("Authorization", "Bearer " + tokenFor("mgmt-test-user"))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("currentPassword" to testPassword, "newPassword" to newPassword))
        }.andExpect {
            // 계정이 비활성화되면 기존 token도 더 이상 인증되지 않는다.
            status { isUnauthorized() }
        }
    }
}
