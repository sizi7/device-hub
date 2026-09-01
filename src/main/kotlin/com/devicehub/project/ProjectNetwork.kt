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
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "project_network")
class ProjectNetwork(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    val project: Project,
    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", nullable = false)
    var environmentType: ProjectEnvironmentType,
    @Column(nullable = false)
    var name: String,
    @Column(name = "api_url", length = 1000)
    var apiUrl: String?,
    @Column(name = "socket_url", length = 1000)
    var socketUrl: String?,
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
