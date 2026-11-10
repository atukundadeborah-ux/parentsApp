package com.example.parentapp.core.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 password hashing for locally-stored [com.example.parentapp.data.local.entity.UserAccount]
 * credentials. Encoded form stored in `passwordHash`:
 *
 * ```
 * <algorithm>:<iterations>:<base64 salt>:<base64 derived key>
 * ```
 *
 * `PBKDF2WithHmacSHA256` is preferred; on API < 26 it may be unavailable, so we fall back
 * to `PBKDF2WithHmacSHA1`. Verification is constant-time. `java.util.Base64` is safe on
 * minSdk 24 thanks to core-library desugaring (and works in plain JVM unit tests).
 */
object PasswordHasher {

    private const val PRIMARY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val FALLBACK_ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256

    private val secureRandom = SecureRandom()
    private val encoder: Base64.Encoder = Base64.getEncoder().withoutPadding()
    private val decoder: Base64.Decoder = Base64.getDecoder()

    fun hash(password: CharArray): String {
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val (algorithm, derived) = derive(password, salt, ITERATIONS)
        return listOf(
            algorithm,
            ITERATIONS.toString(),
            encoder.encodeToString(salt),
            encoder.encodeToString(derived),
        ).joinToString(":")
    }

    fun hash(password: String): String = hash(password.toCharArray())

    /** @return true when [password] matches the stored [encoded] hash. */
    fun verify(password: CharArray, encoded: String): Boolean {
        val parts = encoded.split(":")
        if (parts.size != 4) return false
        val algorithm = parts[0]
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = runCatching { decoder.decode(parts[2]) }.getOrNull() ?: return false
        val expected = runCatching { decoder.decode(parts[3]) }.getOrNull() ?: return false

        val actual = runCatching {
            deriveWith(algorithm, password, salt, iterations, expected.size * 8)
        }.getOrNull() ?: return false

        return constantTimeEquals(expected, actual)
    }

    fun verify(password: String, encoded: String): Boolean = verify(password.toCharArray(), encoded)

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): Pair<String, ByteArray> =
        try {
            PRIMARY_ALGORITHM to deriveWith(PRIMARY_ALGORITHM, password, salt, iterations, KEY_BITS)
        } catch (_: Exception) {
            FALLBACK_ALGORITHM to deriveWith(FALLBACK_ALGORITHM, password, salt, iterations, KEY_BITS)
        }

    private fun deriveWith(
        algorithm: String,
        password: CharArray,
        salt: ByteArray,
        iterations: Int,
        keyBits: Int,
    ): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyBits)
        try {
            return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }
}
