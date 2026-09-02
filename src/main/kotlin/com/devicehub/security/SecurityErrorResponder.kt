package com.devicehub.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * 인증·권한 실패를 HTML 오류 페이지가 아니라 JSON으로 응답한다.
 *
 * 401은 "인증이 없다", 403은 "인증은 되었지만 권한이 없다"로 명확히 구분한다.
 * 응답에는 요청 경로 외에 어떤 내부 정보도 담지 않는다.
 */
@Component
class SecurityErrorResponder(private val objectMapper: ObjectMapper) :
    AuthenticationEntryPoint,
    AccessDeniedHandler {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) = write(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required.")

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) = write(
        response,
        HttpServletResponse.SC_FORBIDDEN,
        "FORBIDDEN",
        "You do not have permission to access this resource.",
    )

    private fun write(response: HttpServletResponse, status: Int, error: String, message: String) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(
            response.outputStream,
            mapOf("status" to status, "error" to error, "message" to message),
        )
    }
}
