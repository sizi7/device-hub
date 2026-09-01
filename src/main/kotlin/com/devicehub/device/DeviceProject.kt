package com.devicehub.device

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

@Entity
@Table(name = "device_project")
class DeviceProject(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    val device: Device,
    @Column(name = "project_name", nullable = false)
    var projectName: String,
    @Column(name = "package_name")
    var packageName: String?,
    @Column(name = "installed_version")
    var installedVersion: String?,
    @Column(name = "latest_version")
    var latestVersion: String?,
    @Column(name = "last_updated_at")
    var lastUpdatedAt: LocalDateTime?,
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
