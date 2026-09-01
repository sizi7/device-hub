package com.devicehub.security

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * DB에 저장하는 비밀값을 AES-GCM으로 암호화한다.
 *
 * 로그인 비밀번호와 달리 키스토어 비밀번호는 실제 APK 서명에 원문이 필요하므로
 * 단방향 해시를 쓸 수 없고, 복호화 가능한 대칭키 암호화를 사용한다.
 * 마스터 키는 devicehub.security.secret-key(환경변수 KEYSTORE_SECRET_KEY)로 주입한다.
 */
@Component
class SecretEncryptor(
    @Value("\${devicehub.security.secret-key}") rawSecretKey: String,
) {
    // 길이가 제각각인 문자열을 SHA-256으로 32byte AES 키로 변환한다.
    private val secretKey = SecretKeySpec(
        MessageDigest.getInstance("SHA-256").digest(rawSecretKey.toByteArray(Charsets.UTF_8)),
        "AES",
    )
    private val random = SecureRandom()

    init {
        if (rawSecretKey == DEFAULT_SECRET_KEY) {
            LoggerFactory.getLogger(SecretEncryptor::class.java).warn(
                "KEYSTORE_SECRET_KEY가 설정되지 않아 개발용 기본 키를 사용합니다. 운영 환경에서는 반드시 별도 값을 설정한다.",
            )
        }
    }

    /** 평문을 암호화해서 "IV + 암호문"을 Base64 한 문자열로 반환한다. */
    fun encrypt(plainText: String): String {
        val iv = ByteArray(IV_LENGTH).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, iv))
        return Base64.getEncoder().encodeToString(iv + cipher.doFinal(plainText.toByteArray(Charsets.UTF_8)))
    }

    /** encrypt로 만든 Base64 문자열을 원래 평문으로 되돌린다. */
    fun decrypt(encoded: String): String {
        val decoded = Base64.getDecoder().decode(encoded)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, decoded, 0, IV_LENGTH))
        return String(cipher.doFinal(decoded, IV_LENGTH, decoded.size - IV_LENGTH), Charsets.UTF_8)
    }

    companion object {
        const val DEFAULT_SECRET_KEY = "devicehub-local-dev-secret-key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BIT = 128
    }
}
