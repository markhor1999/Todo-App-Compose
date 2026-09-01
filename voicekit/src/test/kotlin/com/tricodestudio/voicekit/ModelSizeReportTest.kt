package com.tricodestudio.voicekit

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Prints the real on-disk size of every model. Not an assertion — a report, so the sizes quoted on
 * the landing page can be checked against the actual download manifest instead of being remembered.
 */
class ModelSizeReportTest {
    @Test
    fun reportModelSizes() {
        println("=== VoiceKit model sizes ===")
        VoiceModel.entries.forEach { m ->
            println("${m.name.padEnd(20)} approxSizeMb=${m.approxSizeMb}  files=${m.spec.files.size}  streaming=${m.isStreaming}")
        }
        println("VAD bundle bytes = ${ModelCatalog.vadFile.sizeBytes}")
    }
    /**
     * Pins the sizes that are printed on the landing page and in the KDoc.
     *
     * These were wrong on the first draft of the page — invented rather than read — and three of
     * four were off by 2-4x. A developer discovers that the first time they watch the download,
     * which is the worst possible moment to lose their trust. If a model bundle changes, this test
     * fails and the page copy has to change with it.
     */
    @Test
    fun publishedSizesMatchTheDownloadManifest() {
        assertEquals(153, VoiceModel.MULTILINGUAL.approxSizeMb)
        assertEquals(99, VoiceModel.ENGLISH_FAST.approxSizeMb)
        assertEquals(274, VoiceModel.ENGLISH_COMPACT.approxSizeMb)
        assertEquals(41, VoiceModel.ENGLISH_STREAMING.approxSizeMb)
    }
}
