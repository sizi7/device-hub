package com.devicehub.security

import com.devicehub.user.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authorization: Bearer {token} 헤더를 확인해서 SecurityContext에 인증 정보를 넣는다.
 *
 * token 서명과 만료뿐 아니라 사용자가 아직 존재하고 활성 상태인지, 역할이 바뀌지 않았는지도 확인한다.
 * 유효하지 않으면 인증을 설정하지 않고 그대로 통과시켜서 이후 단계가 401 또는 403으로 처리하게 한다.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)
        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            val payload = jwtTokenProvider.parseToken(token)
            val user = payload?.let { userRepository.findById(it.userId).orElse(null) }
            // token 발급 이후 계정이 비활성화되거나 역할이 바뀐 경우를 막는다.
            if (payload != null && user != null && user.enabled && user.role == payload.role) {
                val principal = AuthenticatedUser(
                    userId = requireNotNull(user.id),
                    username = user.username,
                    role = user.role,
                )
                val authentication = UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    listOf(SimpleGrantedAuthority(user.role.name)),
                )
                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val header = request.getHeader(HEADER_NAME) ?: return null
        if (!header.startsWith(BEARER_PREFIX, ignoreCase = true)) return null
        return header.substring(BEARER_PREFIX.length).trim().takeIf(String::isNotEmpty)
    }

    companion object {
        private const val HEADER_NAME = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
