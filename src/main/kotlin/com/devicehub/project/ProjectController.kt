package com.devicehub.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "독립 프로젝트 마스터 관리 API")
class ProjectController(
    private val projectService: ProjectService,
) {
    @PostMapping
    @Operation(summary = "프로젝트 등록", description = "고유한 code와 프로젝트 기본 정보를 저장합니다.")
    @ApiResponses(value = [ApiResponse(responseCode = "201", description = "등록 성공"), ApiResponse(responseCode = "400", description = "필수 값 또는 code 형식 오류"), ApiResponse(responseCode = "409", description = "code 중복")])
    fun create(@Valid @RequestBody request: ProjectCreateRequest): ResponseEntity<ProjectResponse> {
        val response = projectService.create(request)
        return ResponseEntity.created(URI.create("/api/projects/" + response.id)).body(response)
    }
    @GetMapping
    @Operation(summary = "프로젝트 목록", description = "연결 기기 수와 최근 업로드 APK 요약을 포함해 이름순으로 반환합니다.")
    fun findAll(): ResponseEntity<List<ProjectResponse>> = ResponseEntity.ok(projectService.findAll())
    @GetMapping("/{id}")
    @Operation(summary = "프로젝트 상세", description = "Project ID로 프로젝트 기본 정보와 현재 요약을 조회합니다.")
    fun findById(@Parameter(description = "Project ID", example = "1") @PathVariable id: Long): ResponseEntity<ProjectResponse> =
        ResponseEntity.ok(projectService.findById(id))
    @PutMapping("/{id}")
    @Operation(summary = "프로젝트 수정", description = "프로젝트명, code, 상태, 관리자와 설명을 수정합니다.")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProjectUpdateRequest,
    ): ResponseEntity<ProjectResponse> = ResponseEntity.ok(projectService.update(id, request))
    @DeleteMapping("/{id}")
    @Operation(summary = "프로젝트 삭제", description = "할당·네트워크·APK가 연결된 프로젝트는 안전을 위해 삭제할 수 없습니다.")
    @ApiResponses(value = [ApiResponse(responseCode = "204", description = "삭제 성공"), ApiResponse(responseCode = "404", description = "프로젝트 없음"), ApiResponse(responseCode = "409", description = "연결 데이터가 있어 삭제 불가")])
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        projectService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
