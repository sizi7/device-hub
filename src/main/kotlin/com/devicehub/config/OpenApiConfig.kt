package com.devicehub.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun deviceHubOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("DeviceHub API")
                .description("DeviceHub device management API")
                .version("v1"),
        )
}
