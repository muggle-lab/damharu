package com.mugglelab.damharu.data

import kotlinx.datetime.LocalDate
import kotlin.test.*

class DiaryStoreTest {
    private class MemoryFiles : RecordFiles {
        val data = mutableMapOf("/capture.jpg" to byteArrayOf(1,2,3))
        var failCommit = false
        var failResize = false
        var failCleanup = false
        override fun root() = "/diary"
        override fun read(path: String) = data[path] ?: error("Missing")
        override fun write(path: String, bytes: ByteArray) { if (failCommit && path.endsWith("records.json")) error("Disk full"); data[path] = bytes }
        override fun delete(path: String) { if (failCleanup) error("Cleanup failed"); data.remove(path) }
        override fun exists(path: String) = data.containsKey(path)
        override fun names(path: String) = data.keys.filter { it.startsWith("$path/") }.map { it.substringAfterLast('/') }
        override fun resize(source: String, destination: String, maxSide: Int) {
            if (failResize) error("Encoding failed")
            write(destination, read(source))
        }
    }
    private val today = LocalDate(2026,9,18)
    @Test fun persistReplaceMemoAndDelete() {
        val files = MemoryFiles(); val store = DiaryStore(files)
        assertTrue(store.load().isEmpty())
        val first = store.save(emptyList(), today, "/capture.jpg")
        assertEquals(first, DiaryStore(files).load())
        // One photo per day: a record is exactly one photo and one thumbnail.
        assertEquals(setOf("/capture.jpg", "/diary/records.json", store.path(first.single().image), store.path(first.single().thumbnail)), files.data.keys)
        val memo = store.memo(first, today.toString(), " today\nwas nice ")
        val next = store.save(memo, today, "/capture.jpg")
        assertEquals(1, next.size); assertEquals("today was nice", next.single().memo)
        assertFalse(files.exists(store.path(first.single().image)))
        assertTrue(store.delete(next, today.toString()).isEmpty())
        assertTrue(store.load().isEmpty())
        assertEquals(setOf("/capture.jpg", "/diary/records.json"), files.data.keys)
    }
    @Test fun replacingTodayKeepsOtherDays() {
        val files = MemoryFiles(); val store = DiaryStore(files)
        val yesterday = store.save(emptyList(), LocalDate(2026,9,17), "/capture.jpg")
        val both = store.save(yesterday, today, "/capture.jpg")
        val replaced = store.save(both, today, "/capture.jpg")
        assertEquals(listOf("2026-09-17", "2026-09-18"), replaced.map { it.date })
        assertEquals(yesterday.single(), replaced.first())
        assertNotEquals(both.last().image, replaced.last().image)
    }
    @Test fun failedCommitKeepsPreviousRecordAndFiles() {
        val files = MemoryFiles(); val store = DiaryStore(files)
        val first = store.save(emptyList(), today, "/capture.jpg")
        val before = files.data.keys.toSet()
        files.failCommit = true
        assertFails { store.save(first, today, "/capture.jpg") }
        assertEquals(first, store.load()); assertEquals(before, files.data.keys)
        assertFails { store.delete(first, today.toString()) }; assertEquals(first, store.load())
    }
    @Test fun failedResizeNeverCreatesRecord() {
        val files = MemoryFiles(); files.failResize = true
        val store = DiaryStore(files)
        assertFails { store.save(emptyList(), today, "/capture.jpg") }
        assertTrue(store.load().isEmpty()); assertEquals(setOf("/capture.jpg"), files.data.keys)
    }
    @Test fun cleanupFailureDoesNotUndoSuccessfulCommit() {
        val files = MemoryFiles(); val store = DiaryStore(files)
        val first = store.save(emptyList(), today, "/capture.jpg")
        files.failCleanup = true
        val next = store.save(first, today, "/capture.jpg")
        assertEquals(next, store.load())
    }
    @Test fun legacyFilterCatalogLoadsAndDropsOriginalOnNextCommit() {
        val files = MemoryFiles(); val store = DiaryStore(files)
        files.data["/diary/records.json"] = """{"version":1,"records":[{"date":"2026-09-02","original":"a-original.jpg","image":"a-image.jpg","thumbnail":"a-thumb.jpg","filter":"Warm","intensity":0.6,"memo":"kept"}]}""".encodeToByteArray()
        listOf("a-original.jpg", "a-image.jpg", "a-thumb.jpg").forEach { files.data["/diary/$it"] = byteArrayOf(9) }
        val loaded = store.load()
        assertEquals(listOf(Record("2026-09-02", "a-image.jpg", "a-thumb.jpg", "kept")), loaded)
        store.memo(loaded, "2026-09-02", "still kept")
        assertFalse(files.exists("/diary/a-original.jpg"))
        assertTrue(files.exists("/diary/a-image.jpg")); assertTrue(files.exists("/diary/a-thumb.jpg"))
        assertEquals("still kept", store.load().single().memo)
    }
    @Test fun corruptCatalogIsNotSilentlyReplaced() {
        val files = MemoryFiles(); files.data["/diary/records.json"] = "corrupt".encodeToByteArray()
        assertFails { DiaryStore(files).load() }
        assertEquals("corrupt", files.data.getValue("/diary/records.json").decodeToString())
    }
    @Test fun preventsPathsOutsidePrivateDirectory() {
        assertFails { DiaryStore(MemoryFiles()).path("../photo.jpg") }
    }
}
