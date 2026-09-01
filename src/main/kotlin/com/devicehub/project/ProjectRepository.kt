package com.devicehub.project

import org.springframework.data.jpa.repository.JpaRepository

interface ProjectRepository : JpaRepository<Project, Long> {
    fun existsByCode(code: String): Boolean
    fun existsByCodeAndIdNot(code: String, id: Long): Boolean
}
