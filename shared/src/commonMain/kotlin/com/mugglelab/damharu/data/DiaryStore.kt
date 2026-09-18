package com.mugglelab.damharu.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import kotlin.random.Random

/** OS pickers return an app-owned temporary, orientation-normalized JPEG, or cancellation. */
interface PhotoSource {
    fun request(camera: Boolean, completion: (String?, String?) -> Unit)
}

expect object PhotoFiles {
    fun root(): String
    fun read(path: String): ByteArray
    fun write(path: String, bytes: ByteArray)
    fun delete(path: String)
    fun exists(path: String): Boolean
    fun names(path: String): List<String>
    fun render(source: String, destination: String, matrix: FloatArray, maxSide: Int)
    fun bitmap(path: String): ImageBitmap
}

@Serializable
data class Record(val date: String, val original: String, val image: String, val thumbnail: String,
                  val filter: PhotoFilter = PhotoFilter.Original, val intensity: Float = 1f, val memo: String = "")
@Serializable
private data class Catalog(val version: Int = 1, val records: List<Record> = emptyList())

interface RecordFiles {
    fun root(): String
    fun read(path: String): ByteArray
    fun write(path: String, bytes: ByteArray)
    fun delete(path: String)
    fun exists(path: String): Boolean
    fun names(path: String): List<String>
    fun render(source: String, destination: String, matrix: FloatArray, maxSide: Int)
}
private object DeviceRecordFiles : RecordFiles {
    override fun root() = PhotoFiles.root()
    override fun read(path: String) = PhotoFiles.read(path)
    override fun write(path: String, bytes: ByteArray) = PhotoFiles.write(path, bytes)
    override fun delete(path: String) = PhotoFiles.delete(path)
    override fun exists(path: String) = PhotoFiles.exists(path)
    override fun names(path: String) = PhotoFiles.names(path)
    override fun render(source: String, destination: String, matrix: FloatArray, maxSide: Int) = PhotoFiles.render(source, destination, matrix, maxSide)
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
            require(it.intensity.isFinite() && it.intensity in 0f..1f)
            listOf(it.original, it.image, it.thumbnail).forEach { name -> path(name) }
        }
        return catalog.records.sortedBy { it.date }
    }
    private fun commit(records: List<Record>) {
        files.write(index, json.encodeToString(Catalog(records = records)).encodeToByteArray())
        // Cleanup is best effort AFTER commit; a cleanup failure must not report a failed save.
        val keep = records.flatMap { listOf(it.original, it.image, it.thumbnail) }.toSet()
        runCatching { files.names(root).filter { it.endsWith(".jpg") && it !in keep }.forEach { files.delete(path(it)) } }
    }
    fun save(records: List<Record>, date: LocalDate, source: String, filter: PhotoFilter, intensity: Float): List<Record> {
        require(intensity.isFinite() && intensity in 0f..1f)
        val id = "${date}-${Random.nextLong().toULong()}"
        val record = Record(date.toString(), "$id-original.jpg", "$id-image.jpg", "$id-thumb.jpg", filter, intensity,
            records.find { it.date == date.toString() }?.memo.orEmpty())
        val created = listOf(record.original, record.image, record.thumbnail)
        try {
            files.write(path(record.original), files.read(source))
            files.render(path(record.original), path(record.image), filter.matrix(intensity), 2048)
            files.render(path(record.image), path(record.thumbnail), PhotoFilter.Original.matrix(1f), 180)
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
