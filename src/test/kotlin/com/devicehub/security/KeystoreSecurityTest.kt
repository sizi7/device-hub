package com.devicehub.security

import com.devicehub.user.User
import com.devicehub.user.UserRepository
import com.devicehub.user.UserRole
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional

/**
 * 인증과 키스토어 권한 검증.
 *
 * 테스트에서만 쓰는 계정과 비밀번호를 사용하며 실제 운영 비밀번호를 넣지 않는다.
 * 응답 본문의 비밀번호 값을 단언하거나 출력하지 않고 상태 코드만 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KeystoreSecurityTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val jwtTokenProvider: JwtTokenProvider,
    @Autowired private val objectMapper: ObjectMapper,
) {
    private val testPassword = "security-test-password"

    @BeforeEach
    fun setUp() {
        listOf(
            Triple("sec-test-admin", "보안테스트 관리자", UserRole.ROLE_ADMIN),
            Triple("sec-test-release", "보안테스트 배포담당", UserRole.ROLE_RELEASE_MANAGER),
            Triple("sec-test-user", "보안테스트 일반", UserRole.ROLE_USER),
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

    private fun tokenFor(username: String): String {
        val user = requireNotNull(userRepository.findByUsername(username))
        return jwtTokenProvider.createToken(requireNotNull(user.id), user.username, user.role)
    }

    private fun loginBody(username: String, password: String): String =
        objectMapper.writeValueAsString(mapOf("username" to username, "password" to password))

    @Test
    fun `로그인에 성공하면 access token을 받는다`() {
        mockMvc.post("/api/auth/login") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = loginBody("sec-test-admin", testPassword)
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.tokenType") { value("Bearer") }
            jsonPath("$.user.role") { value("ROLE_ADMIN") }
            // 응답에 비밀번호가 섞여 나오지 않는지 확인한다.
            jsonPath("$.user.password") { doesNotExist() }
        }
    }

    @Test
    fun `비밀번호가 틀리면 401을 반환한다`() {
        mockMvc.post("/api/auth/login") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = loginBody("sec-test-admin", "wrong-password-value")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `없는 사용자로 로그인하면 401을 반환한다`() {
        mockMvc.post("/api/auth/login") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = loginBody("sec-test-nobody", testPassword)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `token이 없으면 키스토어 목록도 401이다`() {
        mockMvc.get("/api/projects/1/keystores").andExpect {
            status { isUnauthorized() }
            jsonPath("$.error") { value("UNAUTHORIZED") }
        }
    }

    @Test
    fun `token이 없으면 reveal은 401이다`() {
        mockMvc.post("/api/projects/1/keystores/1/reveal").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `token이 없으면 download는 401이다`() {
        mockMvc.get("/api/projects/1/keystores/1/download").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `잘못된 token은 401이다`() {
        mockMvc.get("/api/projects/1/keystores") {
            header("Authorization", "Bearer this.is.not-a-valid-token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `ROLE_USER는 reveal에서 403이다`() {
        mockMvc.post("/api/projects/1/keystores/1/reveal") {
            header("Authorization", "Bearer " + tokenFor("sec-test-user"))
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value("FORBIDDEN") }
        }
    }

    @Test
    fun `ROLE_USER는 download에서 403이다`() {
        mockMvc.get("/api/projects/1/keystores/1/download") {
            header("Authorization", "Bearer " + tokenFor("sec-test-user"))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `ROLE_USER는 키스토어 삭제에서 403이다`() {
        mockMvc.post("/api/projects/1/keystores/1/reveal") {
            header("Authorization", "Bearer " + tokenFor("sec-test-user"))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `ROLE_USER도 일반 Device API는 조회할 수 있다`() {
        mockMvc.get("/api/devices") {
            header("Authorization", "Bearer " + tokenFor("sec-test-user"))
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `ROLE_RELEASE_MANAGER는 reveal에서 403이 아니다`() {
        // 대상 키스토어가 없으면 404가 나온다. 권한 단계에서 막히지 않는 것이 확인 대상이다.
        mockMvc.post("/api/projects/999999/keystores/999999/reveal") {
            header("Authorization", "Bearer " + tokenFor("sec-test-release"))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `ROLE_ADMIN은 reveal에서 403이 아니다`() {
        mockMvc.post("/api/projects/999999/keystores/999999/reveal") {
            header("Authorization", "Bearer " + tokenFor("sec-test-admin"))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `ROLE_ADMIN은 download에서 403이 아니다`() {
        mockMvc.get("/api/projects/999999/keystores/999999/download") {
            header("Authorization", "Bearer " + tokenFor("sec-test-admin"))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `ROLE_USER는 사용자 생성에서 403이다`() {
        mockMvc.get("/api/users") {
            header("Authorization", "Bearer " + tokenFor("sec-test-user"))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `health는 인증 없이 열려 있다`() {
        mockMvc.get("/api/health").andExpect {
            status { isOk() }
        }
    }
}
