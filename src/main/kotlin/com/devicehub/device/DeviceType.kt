package com.devicehub.device

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "기기 종류: PHONE(스마트폰), TABLET(태블릿)")
enum class DeviceType {
    PHONE,
    TABLET,
}
