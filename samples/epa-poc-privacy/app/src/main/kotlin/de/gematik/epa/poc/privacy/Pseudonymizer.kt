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

import java.security.MessageDigest

/**
 * Stand-in for the real job number (Arbeitsnummer) generation: values listed in [overrides] map to the
 * given pseudonym, anything else to a hash-suffixed placeholder, so reports and goldens are reproducible.
 * It is not a pseudonymisation scheme: the placeholder still contains the original value.
 */
fun deterministicPseudonymizer(overrides: Map<String, String>): PseudonymizeFunction = { raw ->
    overrides[raw] ?: run {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .take(2)
            .joinToString("") { "%02X".format(it) }
        "PSEUDO-$raw-$hash"
    }
}

/** Remembers every pseudonym handed out in [record] so the report can list them. */
fun PseudonymizeFunction.recordingInto(record: MutableMap<String, String>): PseudonymizeFunction =
    { raw -> this(raw).also { record[raw] = it } }
