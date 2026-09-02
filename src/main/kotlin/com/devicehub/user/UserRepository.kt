package com.devicehub.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): User?
    fun existsByUsername(username: String): Boolean
    fun existsByRole(role: UserRole): Boolean

    /** 대상 사용자를 제외한 활성 관리자 수. 마지막 관리자가 사라지는 변경을 막는 데 사용한다. */
    fun countByRoleAndEnabledTrueAndIdNot(role: UserRole, id: Long): Long
    fun findAllByOrderByIdAsc(): List<User>
}
