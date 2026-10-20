/*
 * Copyright 2024-2026, gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 */

package de.gematik.epa.poc.privacy

import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
    fun generateKey(n: Int): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(n)
        val key = keyGenerator.generateKey()
        return key
    }

    fun deriveKey(password: String, salt: String): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), 65536, 256)
        val secret: SecretKey = SecretKeySpec(
            factory.generateSecret(spec)
                .encoded, "AES"
        )
        return secret
    }

    fun generateIv(): ByteArray {
        val iv = ByteArray(12) // 12 bytes for CGM
        SecureRandom().nextBytes(iv)
        return iv
    }

    fun encryptString(input: String, key: SecretKey): String {
        val iv = generateIv()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val parameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
        val cipherText = cipher.doFinal(input.toByteArray())
        val cipherMessage = iv + cipherText
        return Base64.getUrlEncoder().withoutPadding().encodeToString(cipherMessage)
    }

    fun decryptString(input: String, key: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val cipherMessage = Base64.getUrlDecoder().decode(input)
        val iv = cipherMessage.sliceArray(0 until 12) // First 12 bytes for IV
        val cipherText = cipherMessage.sliceArray(12 until cipherMessage.size)
        val parameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
        val plainText = cipher.doFinal(cipherText)
        return String(plainText)
    }

}