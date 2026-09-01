package com.devicehub.project

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val assignmentRepository: DeviceProjectAssignmentRepository,
    private val projectApkRepository: ProjectApkRepository,
) {
    @Transactional
    fun create(request: ProjectCreateRequest): ProjectResponse {
        val code = request.code.trim()
        if (projectRepository.existsByCode(code)) throw ProjectCodeConflictException(code)
        return projectRepository.save(
            Project(
                name = request.name.trim(),
                code = code,
                description = request.description.normalized(),
                manager = request.manager.normalized(),
                status = requireNotNull(request.status),
            ),
        ).toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<ProjectResponse> =
        projectRepository.findAll().sortedBy { it.name.lowercase() }.map { it.toResponse() }

    @Transactional(readOnly = true)
    fun findById(id: Long): ProjectResponse = findProject(id).toResponse()

    @Transactional
    fun update(id: Long, request: ProjectUpdateRequest): ProjectResponse {
        val project = findProject(id)
        val code = request.code.trim()
        if (projectRepository.existsByCodeAndIdNot(code, id)) throw ProjectCodeConflictException(code)
        project.name = request.name.trim()
        project.code = code
        project.description = request.description.normalized()
        project.manager = request.manager.normalized()
        project.status = requireNotNull(request.status)
        return projectRepository.saveAndFlush(project).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        val project = findProject(id)
        try {
            projectRepository.delete(project)
            projectRepository.flush()
        } catch (exception: DataIntegrityViolationException) {
            throw ProjectDeleteConflictException(id)
        }
    }

    private fun findProject(id: Long): Project =
        projectRepository.findById(id).orElseThrow { ProjectNotFoundException(id) }

    private fun Project.toResponse(): ProjectResponse {
        val latest = projectApkRepository.findFirstByProjectIdOrderByUploadedAtDesc(requireNotNull(id))
        return ProjectResponse(
            id = requireNotNull(id),
            name = name,
            code = code,
            description = description,
            manager = manager,
            status = status,
            connectedDeviceCount = assignmentRepository.countByProjectIdAndEndedAtIsNull(requireNotNull(id)),
            latestApk = latest?.toSummary(),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun ProjectApk.toSummary() = ProjectApkSummaryResponse(
        id = requireNotNull(id),
        version = version,
        versionCode = versionCode,
        environmentType = environmentType,
        uploadedAt = uploadedAt,
    )

    private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}
