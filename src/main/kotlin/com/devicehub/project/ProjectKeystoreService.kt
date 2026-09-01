package com.devicehub.project

import com.devicehub.security.SecretEncryptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.util.UUID

@Service
class ProjectKeystoreService(
    private val projectRepository: ProjectRepository,
    private val projectKeystoreRepository: ProjectKeystoreRepository,
    private val secretEncryptor: SecretEncryptor,
    @Value("\${devicehub.storage.keystore-path}") storagePath: String,
) {
    private val storageRoot: Path = Paths.get(storagePath).toAbsolutePath().normalize()
    private val maxFileSize = 10L * 1024 * 1024
    private val allowedExtensions = listOf(".jks", ".keystore", ".p12", ".pfx")

    @Transactional
    fun upload(
        projectId: Long,
        file: MultipartFile,
        name: String,
        keyAlias: String,
        storePassword: String,
        keyPassword: String?,
        description: String?,
    ): ProjectKeystoreResponse {
        validateFile(file)
        val project = findProject(projectId)
        val projectDirectory = storageRoot.resolve(sanitizeSegment(project.code)).normalize()
        if (!projectDirectory.startsWith(storageRoot)) throw InvalidKeystoreFileException("잘못된 키스토어 저장 경로입니다.")
        Files.createDirectories(projectDirectory)

        val originalName = file.originalFilename?.substringAfterLast('/')?.substringAfterLast('\\')
            ?: throw InvalidKeystoreFileException("키스토어 파일명이 없습니다.")
        val target = projectDirectory.resolve(UUID.randomUUID().toString() + ".keystore").normalize()
        file.inputStream.use { input -> Files.copy(input, target) }

        return try {
            // 실제로 키스토어를 열어 비밀번호와 alias가 맞는지 확인한 뒤에만 저장한다.
            val storeType = detectStoreType(target)
            validateKeyStore(target, storeType, keyAlias, storePassword, keyPassword ?: storePassword)
            projectKeystoreRepository.save(
                ProjectKeystore(
                    project = project,
                    name = name.trim(),
                    fileName = originalName,
                    filePath = target.toString(),
                    storeType = storeType,
                    keyAlias = keyAlias.trim(),
                    storePasswordEnc = secretEncryptor.encrypt(storePassword),
                    keyPasswordEnc = keyPassword.normalized()?.let(secretEncryptor::encrypt),
                    description = description.normalized(),
                ),
            ).toResponse()
        } catch (exception: Exception) {
            Files.deleteIfExists(target)
            throw exception
        }
    }

    @Transactional(readOnly = true)
    fun findAll(projectId: Long): List<ProjectKeystoreResponse> {
        findProject(projectId)
        return projectKeystoreRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findById(projectId: Long, keystoreId: Long): ProjectKeystoreResponse =
        findKeystore(projectId, keystoreId).toResponse()

    /** 저장된 비밀번호를 복호화해서 반환한다. 목록과 상세 응답에는 절대 포함하지 않는 값이다. */
    @Transactional(readOnly = true)
    fun reveal(projectId: Long, keystoreId: Long): ProjectKeystorePasswordResponse {
        val keystore = findKeystore(projectId, keystoreId)
        val storePassword = secretEncryptor.decrypt(keystore.storePasswordEnc)
        return ProjectKeystorePasswordResponse(
            id = requireNotNull(keystore.id),
            keyAlias = keystore.keyAlias,
            storePassword = storePassword,
            keyPassword = keystore.keyPasswordEnc?.let(secretEncryptor::decrypt) ?: storePassword,
        )
    }

    @Transactional
    fun update(projectId: Long, keystoreId: Long, request: ProjectKeystoreUpdateRequest): ProjectKeystoreResponse {
        val keystore = findKeystore(projectId, keystoreId)
        keystore.name = request.name.trim()
        keystore.description = request.description.normalized()
        return projectKeystoreRepository.saveAndFlush(keystore).toResponse()
    }

    @Transactional
    fun updatePassword(
        projectId: Long,
        keystoreId: Long,
        request: ProjectKeystorePasswordRequest,
    ): ProjectKeystoreResponse {
        val keystore = findKeystore(projectId, keystoreId)
        val path = resolveStoredFile(projectId, keystoreId, keystore)
        val keyPassword = request.keyPassword.normalized()
        // 새 비밀번호가 실제 키스토어 파일에서 통하는지 먼저 확인한다.
        validateKeyStore(
            path,
            keystore.storeType,
            request.keyAlias,
            request.storePassword,
            keyPassword ?: request.storePassword,
        )
        keystore.keyAlias = request.keyAlias.trim()
        keystore.storePasswordEnc = secretEncryptor.encrypt(request.storePassword)
        keystore.keyPasswordEnc = keyPassword?.let(secretEncryptor::encrypt)
        return projectKeystoreRepository.saveAndFlush(keystore).toResponse()
    }

    @Transactional(readOnly = true)
    fun download(projectId: Long, keystoreId: Long): ProjectKeystoreDownload {
        val keystore = findKeystore(projectId, keystoreId)
        return ProjectKeystoreDownload(
            keystore.fileName,
            UrlResource(resolveStoredFile(projectId, keystoreId, keystore).toUri()),
        )
    }

    @Transactional
    fun delete(projectId: Long, keystoreId: Long) {
        val keystore = findKeystore(projectId, keystoreId)
        val path = Paths.get(keystore.filePath).toAbsolutePath().normalize()
        if (path.startsWith(storageRoot)) Files.deleteIfExists(path)
        projectKeystoreRepository.delete(keystore)
    }

    private fun validateFile(file: MultipartFile) {
        val name = file.originalFilename ?: throw InvalidKeystoreFileException("키스토어 파일명이 없습니다.")
        if (file.isEmpty) throw InvalidKeystoreFileException("빈 키스토어 파일은 업로드할 수 없습니다.")
        if (allowedExtensions.none { name.lowercase().endsWith(it) }) {
            throw InvalidKeystoreFileException(".jks, .keystore, .p12, .pfx 파일만 업로드할 수 있습니다.")
        }
        if (file.size > maxFileSize) throw InvalidKeystoreFileException("키스토어 파일은 10MB를 초과할 수 없습니다.")
    }

    /**
     * 파일 앞 4byte로 키스토어 형식을 판별한다.
     *
     * JDK 9부터 JKS와 PKCS12 구현이 서로의 형식도 읽어주기 때문에(dual-format 호환),
     * KeyStore.load를 차례로 시도하는 방식으로는 실제 형식을 구분할 수 없다.
     * JKS는 magic number 0xFEEDFEED로 시작하고 PKCS12는 DER SEQUENCE(0x30)로 시작한다.
     */
    private fun detectStoreType(path: Path): String {
        val header = ByteArray(4)
        val read = Files.newInputStream(path).use { input -> input.readNBytes(header, 0, header.size) }
        if (read < header.size) throw InvalidKeystoreFileException("키스토어 파일이 너무 작습니다.")
        val isJks = header[0] == 0xFE.toByte() && header[1] == 0xED.toByte() &&
            header[2] == 0xFE.toByte() && header[3] == 0xED.toByte()
        if (isJks) return "JKS"
        if (header[0] == 0x30.toByte()) return "PKCS12"
        throw InvalidKeystoreFileException("JKS 또는 PKCS12 형식의 키스토어가 아닙니다.")
    }

    private fun validateKeyStore(
        path: Path,
        storeType: String,
        keyAlias: String,
        storePassword: String,
        keyPassword: String,
    ) {
        val keyStore = try {
            loadKeyStore(path, storeType, storePassword)
        } catch (exception: Exception) {
            throw InvalidKeystoreFileException("키스토어를 열 수 없습니다. 파일이 손상되었거나 스토어 비밀번호가 올바르지 않습니다.")
        }
        verifyAliasAndKey(keyStore, keyAlias, keyPassword)
    }

    private fun loadKeyStore(path: Path, storeType: String, storePassword: String): KeyStore {
        val keyStore = KeyStore.getInstance(storeType)
        Files.newInputStream(path).use { input -> keyStore.load(input, storePassword.toCharArray()) }
        return keyStore
    }

    private fun verifyAliasAndKey(keyStore: KeyStore, keyAlias: String, keyPassword: String) {
        val alias = keyAlias.trim()
        if (!keyStore.containsAlias(alias)) {
            throw InvalidKeystoreFileException("키스토어에 alias " + alias + " 가 없습니다.")
        }
        try {
            keyStore.getKey(alias, keyPassword.toCharArray())
        } catch (exception: UnrecoverableKeyException) {
            throw InvalidKeystoreFileException("키 비밀번호가 올바르지 않습니다.")
        }
    }

    private fun resolveStoredFile(projectId: Long, keystoreId: Long, keystore: ProjectKeystore): Path {
        val path = Paths.get(keystore.filePath).toAbsolutePath().normalize()
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
            throw ProjectKeystoreNotFoundException(projectId, keystoreId)
        }
        return path
    }

    private fun findProject(id: Long): Project =
        projectRepository.findById(id).orElseThrow { ProjectNotFoundException(id) }
    private fun findKeystore(projectId: Long, keystoreId: Long): ProjectKeystore =
        projectKeystoreRepository.findByIdAndProjectId(keystoreId, projectId)
            ?: throw ProjectKeystoreNotFoundException(projectId, keystoreId)
    private fun sanitizeSegment(value: String): String =
        value.trim().replace(Regex("[^A-Za-z0-9._-]"), "_").take(100)
    private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)
    private fun ProjectKeystore.toResponse() = ProjectKeystoreResponse(
        id = requireNotNull(id),
        projectId = requireNotNull(project.id),
        projectCode = project.code,
        name = name,
        fileName = fileName,
        storeType = storeType,
        keyAlias = keyAlias,
        hasSeparateKeyPassword = keyPasswordEnc != null,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

data class ProjectKeystoreDownload(val fileName: String, val resource: Resource)
