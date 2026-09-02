package com.devicehub.security

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security 구성.
 *
 * 권한 정책은 두 층으로 나눈다.
 * - 이 파일의 URL 규칙은 "인증이 필요한가"만 정한다. permitAll을 명시한 곳 외에는 모두 인증이 필요하다.
 * - 역할별 제한은 컨트롤러 메서드의 @PreAuthorize가 담당한다.
 *   키스토어는 같은 경로에 HTTP 메서드만 다른 API가 여러 개라서 URL 패턴으로 역할을 나누면 규칙이 어긋나기 쉽다.
 *   권한 규칙을 엔드포인트 바로 옆에 두면 한 곳만 보면 되고 URL 규칙과 이중으로 관리하지 않아도 된다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val securityErrorResponder: SecurityErrorResponder,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Bearer token을 Authorization 헤더로 전달하는 stateless API라서 쿠키 기반 CSRF 공격 경로가 없다.
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .exceptionHandling {
                it.authenticationEntryPoint(securityErrorResponder)
                it.accessDeniedHandler(securityErrorResponder)
            }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                // 컨트롤러가 던진 예외는 ERROR 디스패치로 /error를 한 번 더 거친다.
                // 여기를 막으면 로그인 실패 401이 "인증이 필요하다"는 엉뚱한 메시지로 덮인다.
                it.requestMatchers("/error").permitAll()
                // 운영 프로파일에서는 springdoc 자체를 끄기 때문에 아래 경로는 존재하지 않는다.
                it.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    /** 관리자 웹 개발 서버에서 직접 호출할 때만 필요한 최소 CORS 설정. 와일드카드 origin은 쓰지 않는다. */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:5173", "http://127.0.0.1:5173")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type")
            exposedHeaders = listOf("Content-Disposition")
            allowCredentials = false
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", configuration) }
    }
}
