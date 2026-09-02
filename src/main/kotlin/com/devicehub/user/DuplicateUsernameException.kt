package com.devicehub.user

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateUsernameException(username: String) : RuntimeException("Username already exists: $username")
