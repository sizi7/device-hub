package com.devicehub.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 관리자 웹 로그인 사용자.
 *
 * password에는 BCrypt 해시만 저장한다. 키스토어 비밀번호와 달리 원문이 필요하지 않으므로
 * 복호화 가능한 SecretEncryptor를 쓰지 않고 단방향 해시를 사용한다.
 * user는 PostgreSQL 예약어라서 테이블 이름은 app_user로 둔다.
 */
@Entity
@Table(name = "app_user")
class User(
    @Column(nullable = false, unique = true)
    val username: String,
    @Column(nullable = false, length = 100)
    var password: String,
    @Column(nullable = false)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var role: UserRole,
    @Column(nullable = false)
    var enabled: Boolean = true,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: LocalDateTime
        protected set

    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
