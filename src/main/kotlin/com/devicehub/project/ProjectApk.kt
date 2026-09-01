package com.devicehub.project

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "project_apk")
class ProjectApk(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    val project: Project,
    @Column(nullable = false)
    val version: String,
    @Column(name = "version_code", nullable = false)
    val versionCode: Long,
    @Column(name = "file_name", nullable = false)
    val fileName: String,
    @Column(name = "file_path", nullable = false, length = 1500)
    val filePath: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", nullable = false)
    val environmentType: ProjectEnvironmentType,
    @Column(name = "release_note", columnDefinition = "TEXT")
    val releaseNote: String?,
    @Column(name = "uploaded_at", nullable = false)
    val uploadedAt: LocalDateTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        protected set
    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
    }
}
