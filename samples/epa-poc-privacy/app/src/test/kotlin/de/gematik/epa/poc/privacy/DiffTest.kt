package de.gematik.epa.poc.privacy

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.jetbrains.kotlin.backend.wasm.ir2wasm.toJsStringLiteral
import org.junit.jupiter.api.Test

class DiffTest {
    @Test fun testDiff() {
        val file1 = """
            This is a test file.
            It is used to test the diff utils.
            This is the third line.
            This is the fourth line.
            This is the fifth line.
            This is the sixth line.
            This is the seventh line.
            This is the eighth line.
            This is the ninth line.
            This is the tenth line.
        """.trimIndent()
        val file2 = """
            This is a test file.
            This is the third line.
            This is the fourth line.
            This is the fifth line.
            This is the sixth line.
            This is the seventh line.
            This is the ninth line.
            This is the eleventh line.
        """.trimIndent()

        val patch = DiffUtils.diff(file1.lines(), file2.lines())

        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("file1", "file2", file1.lines(), patch, 3)
        println(unifiedDiff.joinToString("\n"))
    }
}