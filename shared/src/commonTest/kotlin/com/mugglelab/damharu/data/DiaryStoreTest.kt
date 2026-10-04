package com.mugglelab.damharu.data

import kotlinx.datetime.LocalDate
import kotlin.test.*

class DiaryStoreTest {
    private class MemoryFiles : RecordFiles {
        val data = mutableMapOf("/capture.jpg" to byteArrayOf(1,2,3))
        var failCommit = false
        var failResize = false
        var failCleanup = false
        val rendered = mutableListOf<Triple<String, FloatArray?, FilmTexture?>>()
        override fun root() = "/diary"
        override fun read(path: String) = data[path] ?: error("Missing")
        override fun write(path: String, bytes: ByteArray) { if (failCommit && path.endsWith("records.json")) error("Disk full"); data[path] = bytes }
        override fun delete(path: String) { if (failCleanup) error("Cleanup failed"); data.remove(path) }
        override fun exists(path: String) = data.containsKey(path)
        override fun names(path: String) = data.keys.filter { it.startsWith("$path/") }.map { it.substringAfterLast('/') }
        override fun render(source: String, destination: String, matrix: FloatArray?, texture: FilmTexture?, maxSide: Int) {
            if (failResize) error("Encoding failed")
            rendered += Triple(destination.substringAfterLast('/'), matrix, texture)
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
    @Test fun filmIsBakedIntoThePhotoOnceAndRemembered() {
        val files = MemoryFiles(); val store = DiaryStore(files)
        val record = store.save(emptyList(), today, "/capture.jpg", Film.Test).single()
        assertEquals("test", record.film)
        // The photo gets the film matrix; the thumbnail is made from that photo, so it must not be filmed twice.
        assertEquals(listOf(Triple(record.image, Film.Test.matrix, null), Triple(record.thumbnail, null, null)), files.rendered.toList())
        assertEquals(record, DiaryStore(files).load().single())
    }
    @Test fun withoutAFilmThePhotoIsSavedAsCaptured() {
        val files = MemoryFiles(); val store = DiaryStore(files)
        val plain = store.save(emptyList(), today, "/capture.jpg").single()
        assertEquals("", plain.film)
        assertTrue(files.rendered.all { it.second == null && it.third == null }, "no color change, grain or vignette without a film")
    }
    @Test fun vintageBurnsGrainAndVignetteIntoThePhotoOnly() {
        val files = MemoryFiles(); val store = DiaryStore(files)
        val record = store.save(emptyList(), today, "/capture.jpg", Film.Vintage).single()
        assertEquals("vintage", record.film)
        assertEquals(listOf(Triple(record.image, null, Film.Vintage.texture), Triple(record.thumbnail, null, null)), files.rendered.toList())
        // Vintage keeps the captured colors; its look is texture only.
        assertNull(Film.Vintage.matrix); assertNotNull(Film.Vintage.texture)
        assertNull(Film.None.texture); assertNull(Film.Test.texture)
        assertEquals(Film.entries.size, Film.entries.map { it.id }.distinct().size)
    }
    @Test fun grainIsDeterministicAndScalesWithExportSize() {
        val cells = FilmTexture.grainCells
        assertEquals(FilmTexture.GRAIN_SIDE * FilmTexture.GRAIN_SIDE, cells.size)
        assertTrue(cells.all { it >= 0f && it < 1f })
        assertContentEquals(cells, kotlin.random.Random(7).let { random -> FloatArray(cells.size) { random.nextFloat() } })
        assertEquals(1, FilmTexture.grainScale(180)); assertEquals(1, FilmTexture.grainScale(1100)); assertEquals(2, FilmTexture.grainScale(2048))
    }
    @Test fun unknownOrRetiredFilmIdsFallBackToNoFilm() {
        assertEquals(Film.Test, Film.fromId("test")); assertEquals(Film.Vintage, Film.fromId("vintage"))
        assertEquals(Film.None, Film.fromId("")); assertEquals(Film.None, Film.fromId(null)); assertEquals(Film.None, Film.fromId("retired-stock"))
        val files = MemoryFiles()
        files.data["/diary/records.json"] = """{"version":1,"records":[{"date":"2026-09-02","image":"a-photo.jpg","thumbnail":"a-thumb.jpg","film":"retired-stock"}]}""".encodeToByteArray()
        assertEquals("retired-stock", DiaryStore(files).load().single().film)
        assertNull(Film.None.matrix); assertEquals(20, Film.Test.matrix!!.size)
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
        assertEquals(listOf(Record("2026-09-02", "a-image.jpg", "a-thumb.jpg", "kept", "")), loaded)
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
