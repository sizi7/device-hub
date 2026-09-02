package com.devicehub.user

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class UserNotFoundException(id: Long) : RuntimeException("User not found: $id")

/**
 * 허용되지 않는 사용자 변경.
 *
 * 마지막 관리자를 없애거나 본인 계정을 삭제하려는 경우처럼
 * 요청 자체는 올바르지만 시스템 상태 때문에 허용할 수 없는 상황에 사용한다.
 */
@ResponseStatus(HttpStatus.CONFLICT)
class UserOperationNotAllowedException(message: String) : RuntimeException(message)

/** 본인 비밀번호 변경에서 현재 비밀번호가 틀린 경우. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidCurrentPasswordException : RuntimeException("현재 비밀번호가 올바르지 않습니다.")
