package com.devicehub.user

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun create(request: UserCreateRequest): UserResponse {
        val username = request.username.trim()
        if (userRepository.existsByUsername(username)) throw DuplicateUsernameException(username)
        val user = User(
            username = username,
            // 평문 비밀번호는 저장하지도, 로그로 남기지도 않는다.
            password = passwordEncoder.encode(request.password),
            name = request.name.trim(),
            role = requireNotNull(request.role),
        )
        return userRepository.save(user).toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<UserResponse> = userRepository.findAll().map { it.toResponse() }

    private fun User.toResponse(): UserResponse = UserResponse(
        id = requireNotNull(id),
        username = username,
        name = name,
        role = role,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
