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
    val bundlePath: String,
    val sources: List<SourceFile>,
)

@Serializable
data class Config(
    val testCaseList: List<TestCaseConfig>,
)