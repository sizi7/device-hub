package com.devicehub.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SecretEncryptorTest {
    private val encryptor = SecretEncryptor("test-secret-key")

    @Test
    fun `암호화한 값을 복호화하면 원문이 나온다`() {
        val plainText = "storepw123!@#가나다"
        assertEquals(plainText, encryptor.decrypt(encryptor.encrypt(plainText)))
    }

    @Test
    fun `같은 평문도 매번 다른 암호문이 된다`() {
        // 매 암호화마다 새 IV를 쓰기 때문에 같은 비밀번호끼리 비교당하지 않는다.
        val plainText = "storepw123"
        assertNotEquals(encryptor.encrypt(plainText), encryptor.encrypt(plainText))
    }

    @Test
    fun `암호문에 원문이 그대로 남지 않는다`() {
        val plainText = "storepw123"
        val encrypted = encryptor.encrypt(plainText)
        assertEquals(false, encrypted.contains(plainText))
    }

    @Test
    fun `마스터 키가 다르면 복호화할 수 없다`() {
        val encrypted = encryptor.encrypt("storepw123")
        val otherEncryptor = SecretEncryptor("another-secret-key")
        assertThrows(Exception::class.java) { otherEncryptor.decrypt(encrypted) }
    }
}
