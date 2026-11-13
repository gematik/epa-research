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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PseudonymizerTest {

    @Test
    fun `configured values map to their configured pseudonym`() {
        assertEquals(JOB_NUMBER, deterministicPseudonymizer(mapOf(KVNR to JOB_NUMBER))(KVNR))
    }

    @Test
    fun `other values get the same placeholder on every call`() {
        val pseudonymize = deterministicPseudonymizer(emptyMap())

        assertTrue(pseudonymize("A123456789").startsWith("PSEUDO-A123456789-"))
        assertEquals(pseudonymize("A123456789"), pseudonymize("A123456789"))
    }

    @Test
    fun `recording keeps every pseudonym that was handed out`() {
        val record = mutableMapOf<String, String>()
        val pseudonymize = deterministicPseudonymizer(mapOf(KVNR to JOB_NUMBER)).recordingInto(record)

        pseudonymize(KVNR)

        assertEquals(mapOf(KVNR to JOB_NUMBER), record)
    }
}
