package com.devicehub.project

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.UUID

@Service
class ProjectApkService(
    private val projectRepository: ProjectRepository,
    private val projectApkRepository: ProjectApkRepository,
    @Value("\${devicehub.storage.apk-path}") storagePath: String,
) {
    private val storageRoot: Path = Paths.get(storagePath).toAbsolutePath().normalize()
    private val maxFileSize = 200L * 1024 * 1024

    @Transactional
    fun upload(
        projectId: Long,
        file: MultipartFile,
        version: String,
        versionCode: Long,
        environmentType: ProjectEnvironmentType,
        releaseNote: String?,
    ): ProjectApkResponse {
        validateFile(file)
        val project = findProject(projectId)
        val projectDirectory = storageRoot
            .resolve(sanitizeSegment(project.code))
            .resolve(sanitizeSegment(version))
            .normalize()
        if (!projectDirectory.startsWith(storageRoot)) throw InvalidApkFileException("잘못된 APK 저장 경로입니다.")
        Files.createDirectories(projectDirectory)

        val originalName = file.originalFilename?.substringAfterLast('/')?.substringAfterLast('\\')
            ?: throw InvalidApkFileException("APK 파일명이 없습니다.")
        val target = projectDirectory.resolve(UUID.randomUUID().toString() + ".apk").normalize()
        file.inputStream.use { input -> Files.copy(input, target) }

        return try {
            projectApkRepository.save(
                ProjectApk(
                    project = project,
                    version = version.trim(),
                    versionCode = versionCode,
                    fileName = originalName,
                    filePath = target.toString(),
                    environmentType = environmentType,
                    releaseNote = releaseNote.normalized(),
                    uploadedAt = LocalDateTime.now(),
                ),
            ).toResponse()
        } catch (exception: Exception) {
            Files.deleteIfExists(target)
            throw exception
        }
    }

    @Transactional(readOnly = true)
    fun findAll(projectId: Long): List<ProjectApkResponse> {
        findProject(projectId)
        return projectApkRepository.findAllByProjectIdOrderByUploadedAtDesc(projectId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findById(projectId: Long, apkId: Long): ProjectApkResponse = findApk(projectId, apkId).toResponse()

    @Transactional(readOnly = true)
    fun findLatest(projectId: Long, environmentType: ProjectEnvironmentType): LatestProjectApkResponse {
        val project = findProject(projectId)
        val apk = projectApkRepository
            .findFirstByProjectIdAndEnvironmentTypeOrderByUploadedAtDesc(projectId, environmentType)
            ?: throw ProjectApkNotFoundException(projectId, 0)
        return LatestProjectApkResponse(
            projectId = projectId,
            projectName = project.name,
            environmentType = environmentType,
            version = apk.version,
            versionCode = apk.versionCode,
            uploadedAt = apk.uploadedAt,
        )
    }

    @Transactional(readOnly = true)
    fun download(projectId: Long, apkId: Long): ProjectApkDownload {
        val apk = findApk(projectId, apkId)
        val path = Paths.get(apk.filePath).toAbsolutePath().normalize()
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
            throw ProjectApkNotFoundException(projectId, apkId)
        }
        return ProjectApkDownload(apk.fileName, UrlResource(path.toUri()))
    }

    @Transactional
    fun delete(projectId: Long, apkId: Long) {
        val apk = findApk(projectId, apkId)
        val path = Paths.get(apk.filePath).toAbsolutePath().normalize()
        if (path.startsWith(storageRoot)) Files.deleteIfExists(path)
        projectApkRepository.delete(apk)
    }

    private fun validateFile(file: MultipartFile) {
        val name = file.originalFilename ?: throw InvalidApkFileException("APK 파일명이 없습니다.")
        if (file.isEmpty) throw InvalidApkFileException("빈 APK 파일은 업로드할 수 없습니다.")
        if (!name.lowercase().endsWith(".apk")) throw InvalidApkFileException(".apk 파일만 업로드할 수 있습니다.")
        if (file.size > maxFileSize) throw InvalidApkFileException("APK 파일은 200MB를 초과할 수 없습니다.")
    }

    private fun findProject(id: Long): Project =
        projectRepository.findById(id).orElseThrow { ProjectNotFoundException(id) }
    private fun findApk(projectId: Long, apkId: Long): ProjectApk =
        projectApkRepository.findByIdAndProjectId(apkId, projectId)
            ?: throw ProjectApkNotFoundException(projectId, apkId)
    private fun sanitizeSegment(value: String): String =
        value.trim().replace(Regex("[^A-Za-z0-9._-]"), "_").take(100)
    private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)
    private fun ProjectApk.toResponse() = ProjectApkResponse(
        id = requireNotNull(id),
        projectId = requireNotNull(project.id),
        projectName = project.name,
        projectCode = project.code,
        version = version,
        versionCode = versionCode,
        fileName = fileName,
        environmentType = environmentType,
        releaseNote = releaseNote,
        uploadedAt = uploadedAt,
        createdAt = createdAt,
    )
}

data class ProjectApkDownload(val fileName: String, val resource: Resource)
