package com.devicehub.project

import com.devicehub.security.SecurityAuditLogger
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/projects/{projectId}/keystores")
@Tag(name = "Project Keystores", description = "프로젝트 APK 서명 키스토어와 비밀번호 관리 API")
class ProjectKeystoreController(
    private val projectKeystoreService: ProjectKeystoreService,
    private val securityAuditLogger: SecurityAuditLogger,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize(KEYSTORE_MANAGER)
    @Operation(
        summary = "키스토어 등록",
        description = "키스토어 파일은 서버 로컬 storage에 저장하고 비밀번호는 AES-GCM으로 암호화해서 DB에 저장합니다. " +
            "저장 전에 실제로 키스토어를 열어 alias와 비밀번호가 맞는지 검증합니다. 최대 크기는 10MB입니다. " +
            "ROLE_ADMIN 또는 ROLE_RELEASE_MANAGER만 호출할 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "등록 성공"),
            ApiResponse(responseCode = "400", description = "파일, 확장자, 크기, alias 또는 비밀번호 오류"),
            ApiResponse(responseCode = "401", description = "인증 필요"),
            ApiResponse(responseCode = "403", description = "키스토어 관리 권한 필요"),
            ApiResponse(responseCode = "404", description = "프로젝트 없음"),
        ],
    )
    fun upload(
        @PathVariable projectId: Long,
        @Parameter(description = "업로드할 .jks, .keystore, .p12, .pfx 파일") @RequestPart file: MultipartFile,
        @Parameter(description = "키스토어 설정 이름", example = "THYNC Release Keystore") @RequestParam name: String,
        @Parameter(description = "키 alias", example = "release") @RequestParam keyAlias: String,
        @Parameter(description = "스토어 비밀번호") @RequestParam storePassword: String,
        @Parameter(description = "키 비밀번호. 비우면 스토어 비밀번호와 같다고 봅니다.")
        @RequestParam(required = false) keyPassword: String?,
        @Parameter(description = "키스토어 설명") @RequestParam(required = false) description: String?,
    ): ResponseEntity<ProjectKeystoreResponse> {
        val response = try {
            projectKeystoreService.upload(
                projectId,
                file,
                name,
                keyAlias,
                storePassword,
                keyPassword,
                description,
            )
        } catch (exception: Exception) {
            securityAuditLogger.record("KEYSTORE_UPLOAD", projectId, null, success = false)
            throw exception
        }
        securityAuditLogger.record("KEYSTORE_UPLOAD", projectId, response.id, success = true)
        return ResponseEntity.created(URI.create("/api/projects/$projectId/keystores/" + response.id)).body(response)
    }

    @GetMapping
    @Operation(summary = "키스토어 목록", description = "프로젝트 키스토어를 최근 등록순으로 반환합니다. 비밀번호는 포함하지 않습니다.")
    fun findAll(@PathVariable projectId: Long) = ResponseEntity.ok(projectKeystoreService.findAll(projectId))

    @GetMapping("/{keystoreId}")
    @Operation(summary = "키스토어 상세", description = "서버 내부 filePath와 비밀번호를 제외한 키스토어 메타데이터를 반환합니다.")
    fun findById(@PathVariable projectId: Long, @PathVariable keystoreId: Long) =
        ResponseEntity.ok(projectKeystoreService.findById(projectId, keystoreId))

    @PostMapping("/{keystoreId}/reveal")
    @PreAuthorize(KEYSTORE_MANAGER)
    @Operation(
        summary = "비밀번호 조회",
        description = "저장된 비밀번호를 복호화해서 반환합니다. URL과 브라우저 기록에 남지 않도록 GET이 아닌 POST로 제공합니다. " +
            "ROLE_ADMIN 또는 ROLE_RELEASE_MANAGER만 호출할 수 있으며 조회 사실은 감사 로그에 남습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "401", description = "인증 필요"),
            ApiResponse(responseCode = "403", description = "키스토어 관리 권한 필요"),
            ApiResponse(responseCode = "404", description = "프로젝트 또는 키스토어 없음"),
        ],
    )
    fun reveal(
        @PathVariable projectId: Long,
        @PathVariable keystoreId: Long,
    ): ResponseEntity<ProjectKeystorePasswordResponse> {
        val response = try {
            projectKeystoreService.reveal(projectId, keystoreId)
        } catch (exception: Exception) {
            securityAuditLogger.record("KEYSTORE_REVEAL", projectId, keystoreId, success = false)
            throw exception
        }
        securityAuditLogger.record("KEYSTORE_REVEAL", projectId, keystoreId, success = true)
        return ResponseEntity.ok()
            // 복호화한 비밀번호가 브라우저나 중간 캐시에 남지 않도록 한다.
            .cacheControl(CacheControl.noStore())
            .body(response)
    }

    @PutMapping("/{keystoreId}")
    @PreAuthorize(KEYSTORE_MANAGER)
    @Operation(summary = "키스토어 수정", description = "이름과 설명을 수정합니다. 비밀번호는 별도 API로 변경합니다.")
    fun update(
        @PathVariable projectId: Long,
        @PathVariable keystoreId: Long,
        @Valid @RequestBody request: ProjectKeystoreUpdateRequest,
    ) = ResponseEntity.ok(projectKeystoreService.update(projectId, keystoreId, request))

    @PutMapping("/{keystoreId}/password")
    @PreAuthorize(KEYSTORE_MANAGER)
    @Operation(
        summary = "비밀번호 변경",
        description = "저장된 키스토어 파일로 새 alias와 비밀번호를 실제 검증한 뒤 암호화해서 다시 저장합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "변경 성공"),
            ApiResponse(responseCode = "400", description = "alias 또는 비밀번호 오류"),
            ApiResponse(responseCode = "401", description = "인증 필요"),
            ApiResponse(responseCode = "403", description = "키스토어 관리 권한 필요"),
            ApiResponse(responseCode = "404", description = "프로젝트 또는 키스토어 없음"),
        ],
    )
    fun updatePassword(
        @PathVariable projectId: Long,
        @PathVariable keystoreId: Long,
        @Valid @RequestBody request: ProjectKeystorePasswordRequest,
    ): ResponseEntity<ProjectKeystoreResponse> {
        val response = try {
            projectKeystoreService.updatePassword(projectId, keystoreId, request)
        } catch (exception: Exception) {
            securityAuditLogger.record("KEYSTORE_PASSWORD_UPDATE", projectId, keystoreId, success = false)
            throw exception
        }
        securityAuditLogger.record("KEYSTORE_PASSWORD_UPDATE", projectId, keystoreId, success = true)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{keystoreId}/download")
    @PreAuthorize(KEYSTORE_MANAGER)
    @Operation(
        summary = "키스토어 다운로드",
        description = "저장된 키스토어 파일을 원본 파일명의 attachment로 다운로드합니다. " +
            "ROLE_ADMIN 또는 ROLE_RELEASE_MANAGER만 호출할 수 있으며 다운로드 사실은 감사 로그에 남습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "다운로드 성공"),
            ApiResponse(responseCode = "401", description = "인증 필요"),
            ApiResponse(responseCode = "403", description = "키스토어 관리 권한 필요"),
            ApiResponse(responseCode = "404", description = "프로젝트 또는 키스토어 없음"),
        ],
    )
    fun download(@PathVariable projectId: Long, @PathVariable keystoreId: Long): ResponseEntity<Resource> {
        val download = try {
            projectKeystoreService.download(projectId, keystoreId)
        } catch (exception: Exception) {
            securityAuditLogger.record("KEYSTORE_DOWNLOAD", projectId, keystoreId, success = false)
            throw exception
        }
        securityAuditLogger.record("KEYSTORE_DOWNLOAD", projectId, keystoreId, success = true)
        val disposition = ContentDisposition.attachment()
            .filename(download.fileName, StandardCharsets.UTF_8)
            .build()
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(download.resource)
    }

    @DeleteMapping("/{keystoreId}")
    @PreAuthorize(KEYSTORE_MANAGER)
    @Operation(summary = "키스토어 삭제", description = "DB 메타데이터와 서버 로컬 키스토어 파일을 함께 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(responseCode = "401", description = "인증 필요"),
            ApiResponse(responseCode = "403", description = "키스토어 관리 권한 필요"),
            ApiResponse(responseCode = "404", description = "프로젝트 또는 키스토어 없음"),
        ],
    )
    fun delete(@PathVariable projectId: Long, @PathVariable keystoreId: Long): ResponseEntity<Void> {
        try {
            projectKeystoreService.delete(projectId, keystoreId)
        } catch (exception: Exception) {
            securityAuditLogger.record("KEYSTORE_DELETE", projectId, keystoreId, success = false)
            throw exception
        }
        securityAuditLogger.record("KEYSTORE_DELETE", projectId, keystoreId, success = true)
        return ResponseEntity.noContent().build()
    }

    companion object {
        /**
         * 키스토어 파일과 비밀번호를 다룰 수 있는 역할.
         *
         * UserRole enum 이름에 이미 ROLE_ 접두사가 있으므로 접두사를 다시 붙이는 hasAnyRole 대신
         * hasAnyAuthority를 사용한다. 목록과 상세 조회는 인증만 요구해서 일반 사용자도 등록 여부는 확인할 수 있게 둔다.
         */
        private const val KEYSTORE_MANAGER = "hasAnyAuthority('ROLE_ADMIN', 'ROLE_RELEASE_MANAGER')"
    }
}
