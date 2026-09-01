package com.devicehub.project

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectNetworkService(
    private val projectRepository: ProjectRepository,
    private val projectNetworkRepository: ProjectNetworkRepository,
) {
    @Transactional
    fun create(projectId: Long, request: ProjectNetworkRequest): ProjectNetworkResponse {
        val network = ProjectNetwork(
            project = findProject(projectId),
            environmentType = requireNotNull(request.environmentType),
            name = request.name.trim(),
            apiUrl = request.apiUrl.normalized(),
            socketUrl = request.socketUrl.normalized(),
            description = request.description.normalized(),
        )
        return projectNetworkRepository.save(network).toResponse()
    }
    @Transactional(readOnly = true)
    fun findAll(projectId: Long): List<ProjectNetworkResponse> {
        findProject(projectId)
        return projectNetworkRepository.findAllByProjectIdOrderByEnvironmentTypeAsc(projectId).map { it.toResponse() }
    }
    @Transactional(readOnly = true)
    fun findById(projectId: Long, networkId: Long): ProjectNetworkResponse =
        findNetwork(projectId, networkId).toResponse()
    @Transactional
    fun update(projectId: Long, networkId: Long, request: ProjectNetworkRequest): ProjectNetworkResponse {
        val network = findNetwork(projectId, networkId)
        network.environmentType = requireNotNull(request.environmentType)
        network.name = request.name.trim()
        network.apiUrl = request.apiUrl.normalized()
        network.socketUrl = request.socketUrl.normalized()
        network.description = request.description.normalized()
        return projectNetworkRepository.saveAndFlush(network).toResponse()
    }
    @Transactional
    fun delete(projectId: Long, networkId: Long) {
        projectNetworkRepository.delete(findNetwork(projectId, networkId))
    }
    private fun findProject(id: Long): Project =
        projectRepository.findById(id).orElseThrow { ProjectNotFoundException(id) }
    private fun findNetwork(projectId: Long, networkId: Long): ProjectNetwork =
        projectNetworkRepository.findByIdAndProjectId(networkId, projectId)
            ?: throw ProjectNetworkNotFoundException(projectId, networkId)
    private fun ProjectNetwork.toResponse() = ProjectNetworkResponse(
        id = requireNotNull(id),
        projectId = requireNotNull(project.id),
        environmentType = environmentType,
        name = name,
        apiUrl = apiUrl,
        socketUrl = socketUrl,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
    private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}
