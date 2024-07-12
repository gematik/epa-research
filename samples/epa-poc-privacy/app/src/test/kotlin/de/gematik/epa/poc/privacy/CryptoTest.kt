package de.gematik.epa.poc.privacy

import org.junit.jupiter.api.Test
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


class CryptoTest {
    @Test fun testEncryption() {
        val encKey = Crypto.deriveKey("always use secret passwords in production", "and take cale of salt")
        val encrypted = Crypto.encryptString("Hello, World!", encKey)
        println(encrypted)
        val decrypted = Crypto.decryptString(encrypted, encKey)
        println(decrypted)
        assert(decrypted == "Hello, World!")
    }

}