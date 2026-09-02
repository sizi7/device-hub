package com.devicehub.security

import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * 키스토어 조회·다운로드·삭제처럼 민감한 작업의 감사 로그를 남긴다.
 *
 * 남기는 값은 누가, 무엇을, 언제, 성공했는지까지다.
 * 비밀번호, JWT, 마스터 키, 키스토어 파일 내용은 어떤 경우에도 인자로 받지 않는다.
 */
@Component
class SecurityAuditLogger {
    private val logger = LoggerFactory.getLogger("com.devicehub.audit")

    fun record(action: String, projectId: Long, keystoreId: Long?, success: Boolean) {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? AuthenticatedUser
        logger.info(
            "action={}, userId={}, username={}, role={}, projectId={}, keystoreId={}, success={}",
            action,
            principal?.userId,
            principal?.username,
            principal?.role,
            projectId,
            keystoreId,
            success,
        )
    }
}
