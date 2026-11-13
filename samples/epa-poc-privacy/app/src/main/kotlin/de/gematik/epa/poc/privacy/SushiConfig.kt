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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.nio.file.Path
import kotlin.io.path.exists

data class FhirPackage(val name: String, val version: String)

class SushiConfig(sushiProjectDir: Path) {
    val dependencies: List<FhirPackage>

    init {
        val configFile = sushiProjectDir.resolve("sushi-config.yaml")
        require(configFile.exists()) { "File 'sushi-config.yaml' not found in $sushiProjectDir" }

        val dependenciesNode = ObjectMapper(YAMLFactory()).readTree(configFile.toFile())["dependencies"]
        dependencies = dependenciesNode?.fields()?.asSequence().orEmpty()
            .map { (name, node) ->
                // SUSHI accepts both `name: version` and `name: { version: ..., id: ... }`.
                FhirPackage(name, if (node.isTextual) node.textValue() else node["version"].textValue())
            }
            .toList()
    }
}
