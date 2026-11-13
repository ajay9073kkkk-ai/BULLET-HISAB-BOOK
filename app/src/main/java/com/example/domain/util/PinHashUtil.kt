package com.example.domain.util

import java.security.MessageDigest
import java.security.SecureRandom

object PinHashUtil {
    private val random = SecureRandom()

    fun isValidPinFormat(pin: String): Boolean {
        return pin.length == 4 && pin.all { it.isDigit() }
    }

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$pin".toByteArray(Charsets.UTF_8)
        return digest.digest(input).joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
        if (!isValidPinFormat(pin) || salt.isBlank() || expectedHash.isBlank()) return false
        return hashPin(pin, salt) == expectedHash
    }
}
