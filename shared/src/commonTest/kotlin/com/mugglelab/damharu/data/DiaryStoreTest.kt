package com.mugglelab.damharu.data

import kotlinx.datetime.LocalDate
import kotlin.test.*

class DiaryStoreTest {
    private class MemoryFiles : RecordFiles {
        val data = mutableMapOf("/import.jpg" to byteArrayOf(1,2,3))
        var failCommit = false
        var failRender = false
        var failCleanup = false
        override fun root() = "/diary"
        override fun read(path: String) = data[path] ?: error("Missing")
        override fun write(path: String, bytes: ByteArray) { if (failCommit && path.endsWith("records.json")) error("Disk full"); data[path] = bytes }
        override fun delete(path: String) { if (failCleanup) error("Cleanup failed"); data.remove(path) }
        override fun exists(path: String) = data.containsKey(path)
        override fun names(path: String) = data.keys.filter { it.startsWith("$path/") }.map { it.substringAfterLast('/') }
        override fun render(source: String, destination: String, matrix: FloatArray, maxSide: Int) {
            if (failRender) error("Encoding failed")
            write(destination, read(source))
        }
    }
    private val date = LocalDate(2026,9,18)
    @Test fun persistReplaceMemoAndDelete() {
        val files = MemoryFiles(); val store = DiaryStore(files)
        assertTrue(store.load().isEmpty())
        val first = store.save(emptyList(), date, "/import.jpg", PhotoFilter.Warm, .6f)
        assertEquals(first, DiaryStore(files).load())
        val memo = store.memo(first, date.toString(), " today\nwas nice ")
        val next = store.save(memo, date, "/import.jpg", PhotoFilter.Mono, .4f)
        assertEquals(1, next.size); assertEquals("today was nice", next.single().memo)
        assertFalse(files.exists(store.path(first.single().original)))
        assertTrue(store.delete(next, date.toString()).isEmpty())
        assertTrue(store.load().isEmpty())
        assertEquals(setOf("/import.jpg", "/diary/records.json"), files.data.keys)
    }
    @Test fun failedCommitKeepsPreviousRecordAndFiles() {
        val files = MemoryFiles(); val store = DiaryStore(files)
        val first = store.save(emptyList(), date, "/import.jpg", PhotoFilter.Original, 1f)
        val before = files.data.keys.toSet()
        files.failCommit = true
        assertFails { store.save(first, date, "/import.jpg", PhotoFilter.Film, 1f) }
        assertEquals(first, store.load()); assertEquals(before, files.data.keys)
        assertFails { store.delete(first, date.toString()) }; assertEquals(first, store.load())
    }
    @Test fun failedRenderNeverCreatesRecord() {
        val files = MemoryFiles(); files.failRender = true
        val store = DiaryStore(files)
        assertFails { store.save(emptyList(), date, "/import.jpg", PhotoFilter.Clear, 1f) }
        assertTrue(store.load().isEmpty()); assertEquals(setOf("/import.jpg"), files.data.keys)
    }
    @Test fun cleanupFailureDoesNotUndoSuccessfulCommit() {
        val files = MemoryFiles(); val store = DiaryStore(files)
        val first = store.save(emptyList(), date, "/import.jpg", PhotoFilter.Original, 1f)
        files.failCleanup = true
        val next = store.save(first, date, "/import.jpg", PhotoFilter.Warm, 1f)
        assertEquals(next, store.load())
    }
    @Test fun corruptCatalogIsNotSilentlyReplaced() {
        val files = MemoryFiles(); files.data["/diary/records.json"] = "corrupt".encodeToByteArray()
        assertFails { DiaryStore(files).load() }
        assertEquals("corrupt", files.data.getValue("/diary/records.json").decodeToString())
    }
    @Test fun preventsPathsOutsidePrivateDirectory() {
        assertFails { DiaryStore(MemoryFiles()).path("../photo.jpg") }
    }
    @Test fun zeroIntensityAndOriginalPreservePixels() {
        val identity = PhotoFilter.Original.matrix(1f)
        PhotoFilter.entries.forEach { assertContentEquals(identity, it.matrix(0f)) }
        assertContentEquals(identity, PhotoFilter.Original.matrix(.7f))
        val gray = PhotoFilter.Mono.matrix(1f)
        assertContentEquals(gray.sliceArray(0..4), gray.sliceArray(5..9))
        assertContentEquals(gray.sliceArray(0..4), gray.sliceArray(10..14))
        assertEquals(1f, gray[18])
    }
}
