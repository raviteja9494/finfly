/* Unit tests for exact Gemma download completion validation. */
package com.teja.finflyiii.data.ai

import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaModelSpecTest {
    @Test
    fun modelIsReadyOnlyAtPublishedByteSize() {
        val file = File.createTempFile("gemma-test", ".litertlm")
        try {
            RandomAccessFile(file, "rw").use { it.setLength(GemmaModelSpec.EXPECTED_SIZE_BYTES - 1) }
            assertFalse(GemmaModelSpec.isComplete(file))

            RandomAccessFile(file, "rw").use { it.setLength(GemmaModelSpec.EXPECTED_SIZE_BYTES) }
            assertTrue(GemmaModelSpec.isComplete(file))

            RandomAccessFile(file, "rw").use { it.setLength(GemmaModelSpec.EXPECTED_SIZE_BYTES + 1) }
            assertFalse(GemmaModelSpec.isComplete(file))
        } finally {
            file.delete()
        }
    }
}
