package com.devicehub.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun deviceHubOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("DeviceHub API")
                .description(
                    "DeviceHub device management API. " +
                        "POST /api/auth/login 으로 받은 accessToken을 오른쪽 위 Authorize 버튼에 입력하면 " +
                        "이후 요청에 Authorization: Bearer {token} 헤더가 자동으로 붙습니다.",
                )
                .version("v1"),
        )
        // 로그인과 health를 제외한 모든 API에 Bearer 인증이 필요하다는 것을 문서 전역 기본값으로 표시한다.
        .addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))
        .components(
            Components().addSecuritySchemes(
                BEARER_SCHEME,
                SecurityScheme()
                    .name(BEARER_SCHEME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("로그인 응답의 accessToken 값을 입력합니다. Bearer 접두사는 자동으로 붙습니다."),
            ),
        )

    companion object {
        private const val BEARER_SCHEME = "bearerAuth"
    }
}
