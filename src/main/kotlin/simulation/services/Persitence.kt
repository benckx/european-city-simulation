package simulation.services

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import simulation.model.Layout
import java.io.File

private val json by lazy {
    Json {
        prettyPrint = false
        encodeDefaults = false
    }
}

private const val LAYOUTS_DIR = "layouts"

fun outputToJson(layout: Layout, fileName: String = "layout") {
    val jsonString = json.encodeToString(layout)

    val layoutsDir = File(LAYOUTS_DIR)
    if (!layoutsDir.exists()) {
        layoutsDir.mkdirs()
    }

    val jsonFile = File(layoutsDir, "$fileName.json")
    jsonFile.writeText(jsonString)
}

fun listFiles(): List<String> =
    (File(LAYOUTS_DIR).listFiles()?.map { it.name } ?: emptyList())
        .map { fileName -> fileName.removeSuffix(".json") }

fun readLayoutFromJson(fileName: String): Layout {
    val jsonFile = File(LAYOUTS_DIR, "${fileName}.json")
    val jsonString = jsonFile.readText()
    return json.decodeFromString(Layout.serializer(), jsonString)
}
