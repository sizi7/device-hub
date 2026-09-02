package com.devicehub.user

import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

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
        val saved = userRepository.save(user)
        logger.info("사용자 생성: username={}, role={}", saved.username, saved.role)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<UserResponse> = userRepository.findAllByOrderByIdAsc().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun findById(id: Long): UserResponse = findUser(id).toResponse()

    @Transactional
    fun update(id: Long, request: UserUpdateRequest): UserResponse {
        val user = findUser(id)
        val role = requireNotNull(request.role)
        val enabled = requireNotNull(request.enabled)
        // 역할 강등이나 비활성화로 활성 관리자가 0명이 되면 아무도 시스템을 관리할 수 없게 된다.
        requireRemainingAdmin(user, willBeEnabledAdmin = role == UserRole.ROLE_ADMIN && enabled)
        user.name = request.name.trim()
        user.role = role
        user.enabled = enabled
        val saved = userRepository.saveAndFlush(user)
        logger.info("사용자 수정: username={}, role={}, enabled={}", saved.username, saved.role, saved.enabled)
        return saved.toResponse()
    }

    /** 관리자가 다른 사용자의 비밀번호를 재설정한다. 현재 비밀번호는 확인하지 않는다. */
    @Transactional
    fun resetPassword(id: Long, request: UserPasswordResetRequest): UserResponse {
        val user = findUser(id)
        user.password = passwordEncoder.encode(request.newPassword)
        val saved = userRepository.saveAndFlush(user)
        logger.info("관리자에 의한 비밀번호 재설정: username={}", saved.username)
        return saved.toResponse()
    }

    /** 로그인한 사용자가 본인 비밀번호를 바꾼다. 현재 비밀번호를 함께 확인한다. */
    @Transactional
    fun changeOwnPassword(userId: Long, request: PasswordChangeRequest): UserResponse {
        val user = findUser(userId)
        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            logger.info("본인 비밀번호 변경 실패: username={}", user.username)
            throw InvalidCurrentPasswordException()
        }
        user.password = passwordEncoder.encode(request.newPassword)
        val saved = userRepository.saveAndFlush(user)
        logger.info("본인 비밀번호 변경: username={}", saved.username)
        return saved.toResponse()
    }

    @Transactional
    fun delete(id: Long, currentUserId: Long) {
        val user = findUser(id)
        if (id == currentUserId) {
            throw UserOperationNotAllowedException("본인 계정은 삭제할 수 없습니다.")
        }
        requireRemainingAdmin(user, willBeEnabledAdmin = false)
        logger.info("사용자 삭제: username={}", user.username)
        userRepository.delete(user)
    }

    private fun requireRemainingAdmin(user: User, willBeEnabledAdmin: Boolean) {
        // 지금 활성 관리자인 사람이 그 자리에서 내려오는 경우에만 확인한다.
        // 일반 사용자를 지우거나 수정하는 요청은 관리자 수와 무관하다.
        val isCurrentlyEnabledAdmin = user.role == UserRole.ROLE_ADMIN && user.enabled
        if (!isCurrentlyEnabledAdmin || willBeEnabledAdmin) return
        val otherAdmins = userRepository.countByRoleAndEnabledTrueAndIdNot(
            UserRole.ROLE_ADMIN,
            requireNotNull(user.id),
        )
        if (otherAdmins == 0L) {
            throw UserOperationNotAllowedException("활성 관리자가 최소 한 명은 있어야 합니다.")
        }
    }

    private fun findUser(id: Long): User =
        userRepository.findById(id).orElseThrow { UserNotFoundException(id) }

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
