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