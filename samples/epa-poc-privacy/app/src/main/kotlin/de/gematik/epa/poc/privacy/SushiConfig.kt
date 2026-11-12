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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.nio.file.Path

class SushiConfig(sushiProjectDir: Path) {
    inner class SushiDependency(val packageName: String, val packageVersion: String)

    private val rootNode: JsonNode

    init {
        val sushiConfigPath = sushiProjectDir.resolve("sushi-config.yaml")
        if (!sushiConfigPath.toFile().exists()) {
            throw Exception(String.format("File 'sushi-config.yaml' not found in %s", sushiProjectDir))
        }

        val om = ObjectMapper(YAMLFactory())
        this.rootNode = om.readTree(sushiConfigPath.toFile())
    }

    val dependencies: Collection<SushiDependency>
        get() {
            val dependenciesNode = rootNode["dependencies"] ?: return emptyList()
            val packageNames = dependenciesNode.fieldNames()
            val dependencies = ArrayList<SushiDependency>()
            while (packageNames.hasNext()) {
                val packageName = packageNames.next()
                val dependencyNode = dependenciesNode[packageName]
                if (dependencyNode.isTextual) {
                    dependencies.add(SushiDependency(packageName, dependencyNode.textValue()))
                } else {
                    dependencies.add(SushiDependency(packageName, dependencyNode["version"].textValue()))
                }
            }

            return dependencies
        }
}