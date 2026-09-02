package com.devicehub.user

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 사용자 역할.
 *
 * Spring Security의 hasRole은 "ROLE_" 접두사를 자동으로 붙이기 때문에 접두사가 두 번 붙는 실수가 생기기 쉽다.
 * 그래서 enum 이름 자체에 ROLE_ 접두사를 포함해 DB에 그대로 저장하고,
 * 권한 검사는 접두사를 붙이지 않는 hasAnyAuthority로만 통일한다.
 */
@Schema(description = "사용자 역할: ROLE_ADMIN(전체 관리), ROLE_RELEASE_MANAGER(배포·키스토어 관리), ROLE_USER(일반 조회)")
enum class UserRole {
    ROLE_ADMIN,
    ROLE_RELEASE_MANAGER,
    ROLE_USER,
    ;

    companion object {
        /** 키스토어 파일과 비밀번호에 접근할 수 있는 역할. */
        val KEYSTORE_MANAGERS = setOf(ROLE_ADMIN, ROLE_RELEASE_MANAGER)
    }
}
