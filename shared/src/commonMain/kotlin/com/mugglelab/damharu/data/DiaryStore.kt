package com.mugglelab.damharu.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import kotlin.random.Random

/** The in-app camera is the only photo input. It returns an app-owned temporary,
 *  orientation-normalized JPEG path, a failure message, or (null, null) when the user closed it. */
interface PhotoSource {
    fun capture(completion: (String?, String?) -> Unit)
}

expect object PhotoFiles {
    fun root(): String
    fun read(path: String): ByteArray
    fun write(path: String, bytes: ByteArray)
    fun delete(path: String)
    fun exists(path: String): Boolean
    fun names(path: String): List<String>
    fun resize(source: String, destination: String, maxSide: Int)
    fun bitmap(path: String): ImageBitmap
}

/** One photo per day. Catalogs written before filters were removed also carry original/filter/intensity;
 *  those keys are ignored on load, the rendered image becomes the photo, and cleanup drops the old original. */
@Serializable
data class Record(val date: String, val image: String, val thumbnail: String, val memo: String = "")
@Serializable
private data class Catalog(val version: Int = 1, val records: List<Record> = emptyList())

interface RecordFiles {
    fun root(): String
    fun read(path: String): ByteArray
    fun write(path: String, bytes: ByteArray)
    fun delete(path: String)
    fun exists(path: String): Boolean
    fun names(path: String): List<String>
    fun resize(source: String, destination: String, maxSide: Int)
}
private object DeviceRecordFiles : RecordFiles {
    override fun root() = PhotoFiles.root()
    override fun read(path: String) = PhotoFiles.read(path)
    override fun write(path: String, bytes: ByteArray) = PhotoFiles.write(path, bytes)
    override fun delete(path: String) = PhotoFiles.delete(path)
    override fun exists(path: String) = PhotoFiles.exists(path)
    override fun names(path: String) = PhotoFiles.names(path)
    override fun resize(source: String, destination: String, maxSide: Int) = PhotoFiles.resize(source, destination, maxSide)
}

/** Image files are immutable. Atomically replacing the catalog is the commit point. */
class DiaryStore(private val files: RecordFiles = DeviceRecordFiles) {
    private val json = Json { ignoreUnknownKeys = true }
    private val root get() = files.root()
    private val index get() = "$root/records.json"
    fun path(name: String): String {
        require(name.matches(Regex("[a-zA-Z0-9_.-]+"))) { "Invalid local file name" }
        return "$root/$name"
    }
    fun load(): List<Record> {
        if (!files.exists(index)) return emptyList()
        val catalog = json.decodeFromString<Catalog>(files.read(index).decodeToString())
        require(catalog.version == 1) { "Unsupported catalog version" }
        require(catalog.records.map { it.date }.distinct().size == catalog.records.size)
        catalog.records.forEach {
            LocalDate.parse(it.date)
            listOf(it.image, it.thumbnail).forEach { name -> path(name) }
        }
        return catalog.records.sortedBy { it.date }
    }
    private fun commit(records: List<Record>) {
        files.write(index, json.encodeToString(Catalog(records = records)).encodeToByteArray())
        // Cleanup is best effort AFTER commit; a cleanup failure must not report a failed save.
        val keep = records.flatMap { listOf(it.image, it.thumbnail) }.toSet()
        runCatching { files.names(root).filter { it.endsWith(".jpg") && it !in keep }.forEach { files.delete(path(it)) } }
    }
    /** Records can only be created or replaced for today; past days stay as they are. */
    fun save(records: List<Record>, today: LocalDate, source: String): List<Record> {
        val id = "${today}-${Random.nextLong().toULong()}"
        val record = Record(today.toString(), "$id-photo.jpg", "$id-thumb.jpg",
            records.find { it.date == today.toString() }?.memo.orEmpty())
        val created = listOf(record.image, record.thumbnail)
        try {
            files.resize(source, path(record.image), 2048)
            files.resize(path(record.image), path(record.thumbnail), 180)
            val next = (records.filterNot { it.date == record.date } + record).sortedBy { it.date }
            commit(next)
            return next
        } catch (error: Exception) {
            created.forEach { runCatching { files.delete(path(it)) } }
            throw error
        }
    }
    fun memo(records: List<Record>, date: String, value: String): List<Record> {
        val next = records.map { if (it.date == date) it.copy(memo = value.replace("\n", " ").take(120).trim()) else it }
        commit(next)
        return next
    }
    fun delete(records: List<Record>, date: String): List<Record> {
        val next = records.filterNot { it.date == date }
        commit(next)
        return next
    }
}
