package com.devicehub.security

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * 로그인 실패.
 *
 * username이 없는 경우와 비밀번호가 틀린 경우를 구분하지 않는다.
 * 구분해서 알려주면 어떤 계정이 존재하는지 알려주는 셈이 된다.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
class AuthenticationFailedException : RuntimeException("Invalid username or password.")

/** 비활성화된 계정. 자격 증명은 맞지만 사용할 수 없는 상태다. */
@ResponseStatus(HttpStatus.FORBIDDEN)
class AccountDisabledException : RuntimeException("This account is disabled.")
