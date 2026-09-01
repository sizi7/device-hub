package com.devicehub.project

import io.swagger.v3.oas.annotations.Operation
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
@RequestMapping("/api/projects/{projectId}/networks")
@Tag(name = "Project Networks", description = "프로젝트 환경별 API와 Socket URL 관리 API")
class ProjectNetworkController(private val projectNetworkService: ProjectNetworkService) {
    @PostMapping
    @Operation(summary = "네트워크 등록", description = "ISO, MFDS, DEVELOPMENT, BUSINESS 환경의 API/Socket URL을 저장합니다. Secret과 Token은 받지 않습니다.")
    @ApiResponses(value = [ApiResponse(responseCode = "201", description = "등록 성공"), ApiResponse(responseCode = "400", description = "필수 값 오류"), ApiResponse(responseCode = "404", description = "프로젝트 없음")])
    fun create(
        @PathVariable projectId: Long,
        @Valid @RequestBody request: ProjectNetworkRequest,
    ): ResponseEntity<ProjectNetworkResponse> {
        val response = projectNetworkService.create(projectId, request)
        return ResponseEntity.created(URI.create("/api/projects/$projectId/networks/" + response.id)).body(response)
    }
    @GetMapping
    @Operation(summary = "네트워크 목록", description = "프로젝트에 등록된 네트워크 설정을 환경순으로 조회합니다.")
    fun findAll(@PathVariable projectId: Long) = ResponseEntity.ok(projectNetworkService.findAll(projectId))
    @GetMapping("/{networkId}")
    @Operation(summary = "네트워크 상세", description = "해당 프로젝트 소유의 네트워크 설정 한 건을 조회합니다.")
    fun findById(@PathVariable projectId: Long, @PathVariable networkId: Long) =
        ResponseEntity.ok(projectNetworkService.findById(projectId, networkId))
    @PutMapping("/{networkId}")
    @Operation(summary = "네트워크 수정", description = "환경, 이름, URL과 설명을 수정합니다.")
    fun update(
        @PathVariable projectId: Long,
        @PathVariable networkId: Long,
        @Valid @RequestBody request: ProjectNetworkRequest,
    ) = ResponseEntity.ok(projectNetworkService.update(projectId, networkId, request))
    @DeleteMapping("/{networkId}")
    @Operation(summary = "네트워크 삭제", description = "프로젝트의 네트워크 설정 한 건을 삭제합니다.")
    fun delete(@PathVariable projectId: Long, @PathVariable networkId: Long): ResponseEntity<Void> {
        projectNetworkService.delete(projectId, networkId)
        return ResponseEntity.noContent().build()
    }
}
