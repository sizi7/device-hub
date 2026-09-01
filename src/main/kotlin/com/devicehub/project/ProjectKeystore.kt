package com.devicehub.project

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 프로젝트별 APK 서명 키스토어.
 *
 * 키스토어 파일 자체는 서버 로컬 storage에 저장하고 DB에는 경로만 남긴다.
 * 비밀번호는 SecretEncryptor로 암호화한 문자열만 저장하며 평문은 DB에 남기지 않는다.
 */
@Entity
@Table(name = "project_keystore")
class ProjectKeystore(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    val project: Project,
    @Column(nullable = false)
    var name: String,
    @Column(name = "file_name", nullable = false)
    val fileName: String,
    @Column(name = "file_path", nullable = false, length = 1500)
    val filePath: String,
    @Column(name = "store_type", nullable = false, length = 20)
    val storeType: String,
    @Column(name = "key_alias", nullable = false)
    var keyAlias: String,
    @Column(name = "store_password_enc", nullable = false, length = 1000)
    var storePasswordEnc: String,
    @Column(name = "key_password_enc", length = 1000)
    var keyPasswordEnc: String?,
    @Column(columnDefinition = "TEXT")
    var description: String?,
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
