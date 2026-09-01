package com.devicehub.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/projects/{projectId}/apks")
@Tag(name = "Project APKs", description = "프로젝트 환경별 APK 업로드, 조회와 다운로드 API")
class ProjectApkController(private val projectApkService: ProjectApkService) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "APK 업로드", description = ".apk 파일은 서버 로컬 storage에 저장하고 DB에는 버전과 파일 메타데이터만 저장합니다. 최대 크기는 200MB입니다.")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "업로드 성공"), ApiResponse(responseCode = "400", description = "파일, 확장자, 크기 또는 파라미터 오류"), ApiResponse(responseCode = "404", description = "프로젝트 없음")])
    fun upload(
        @PathVariable projectId: Long,
        @Parameter(description = "업로드할 .apk 파일") @RequestPart file: MultipartFile,
        @Parameter(description = "Android versionName", example = "1.5.2") @RequestParam version: String,
        @Parameter(description = "Android versionCode", example = "152") @RequestParam versionCode: Long,
        @Parameter(description = "배포 환경", example = "ISO") @RequestParam environmentType: ProjectEnvironmentType,
        @Parameter(description = "릴리즈 노트") @RequestParam(required = false) releaseNote: String?,
    ): ResponseEntity<ProjectApkResponse> =
        ResponseEntity.ok(projectApkService.upload(projectId, file, version, versionCode, environmentType, releaseNote))

    @GetMapping
    @Operation(summary = "APK 목록", description = "프로젝트 APK 메타데이터를 최근 업로드순으로 반환합니다.")
    fun findAll(@PathVariable projectId: Long) = ResponseEntity.ok(projectApkService.findAll(projectId))
    @GetMapping("/latest")
    @Operation(summary = "환경별 최신 APK", description = "선택한 환경에서 uploadedAt이 가장 최근인 APK를 반환합니다.")
    fun findLatest(
        @PathVariable projectId: Long,
        @RequestParam environmentType: ProjectEnvironmentType,
    ) = ResponseEntity.ok(projectApkService.findLatest(projectId, environmentType))
    @GetMapping("/{apkId}")
    @Operation(summary = "APK 메타데이터 상세", description = "서버 내부 filePath와 binary를 제외한 APK 메타데이터를 반환합니다.")
    fun findById(@PathVariable projectId: Long, @PathVariable apkId: Long) =
        ResponseEntity.ok(projectApkService.findById(projectId, apkId))
    @GetMapping("/{apkId}/download")
    @Operation(summary = "APK 다운로드", description = "저장된 APK binary를 원본 파일명의 attachment로 다운로드합니다.")
    fun download(@PathVariable projectId: Long, @PathVariable apkId: Long): ResponseEntity<Resource> {
        val download = projectApkService.download(projectId, apkId)
        val disposition = ContentDisposition.attachment()
            .filename(download.fileName, StandardCharsets.UTF_8)
            .build()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
            .body(download.resource)
    }
    @DeleteMapping("/{apkId}")
    @Operation(summary = "APK 삭제", description = "DB 메타데이터와 서버 로컬 APK 파일을 함께 삭제합니다.")
    fun delete(@PathVariable projectId: Long, @PathVariable apkId: Long): ResponseEntity<Void> {
        projectApkService.delete(projectId, apkId)
        return ResponseEntity.noContent().build()
    }
}
