package com.devicehub.device

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
@Table(name = "device_deployment")
class DeviceDeployment(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    val device: Device,

    @Column(name = "hospital_name", nullable = false)
    var hospitalName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "deployment_type", nullable = false)
    var deploymentType: DeploymentType,

    @Column(name = "deployed_at", nullable = false)
    var deployedAt: LocalDateTime,

    @Column(name = "note", columnDefinition = "TEXT")
    var note: String?,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "returned_at")
    var returnedAt: LocalDateTime? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
    }
}
