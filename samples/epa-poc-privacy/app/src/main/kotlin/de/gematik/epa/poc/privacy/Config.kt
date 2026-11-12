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

import kotlinx.serialization.Serializable

@Serializable
data class SourceFile(
    val title: String,
    val path: String,
    val language: String,
    var content: String? = null,
)

@Serializable
data class TestCaseConfig(
    val id: String,
    val title: String,
    val sushiProjectPath: String,
    val resourcePaths: List<String>,
    val expectedResearchPaths: List<String> = emptyList(),
    val pseudonyms: Map<String, String> = emptyMap(),
    val sources: List<SourceFile> = emptyList(),
)

@Serializable
data class Config(
    val testCaseList: List<TestCaseConfig>,
)
