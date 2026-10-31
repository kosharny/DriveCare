package com.example.drivecare.data.catalogue

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class CatalogueResult(val names: List<String>, val online: Boolean)

object VehicleSuggestions {
    private val models =
        linkedMapOf(
            "Audi" to listOf("A3", "A4", "A5", "A6", "Q3", "Q5", "Q7"),
            "BMW" to listOf("1 Series", "3 Series", "5 Series", "X1", "X3", "X5"),
            "Chevrolet" to listOf("Aveo", "Cruze", "Lacetti", "Malibu", "Captiva"),
            "Citroën" to listOf("C3", "C4", "C5", "Berlingo"),
            "Dacia" to listOf("Duster", "Logan", "Sandero"),
            "Fiat" to listOf("500", "Panda", "Punto", "Tipo"),
            "Ford" to listOf("Fiesta", "Focus", "Mondeo", "Kuga", "Escape"),
            "Honda" to listOf("Civic", "Accord", "CR-V", "HR-V", "Jazz"),
            "Hyundai" to listOf("i20", "i30", "Elantra", "Sonata", "Tucson", "Santa Fe"),
            "Kia" to listOf("Ceed", "Rio", "Cerato", "Sportage", "Sorento"),
            "Lexus" to listOf("IS", "ES", "NX", "RX", "GX"),
            "Mazda" to listOf("2", "3", "6", "CX-3", "CX-5", "CX-9"),
            "Mercedes-Benz" to listOf("A-Class", "C-Class", "E-Class", "GLA", "GLC", "GLE"),
            "Mitsubishi" to listOf("Lancer", "ASX", "Outlander", "Pajero"),
            "Nissan" to listOf("Micra", "Juke", "Qashqai", "X-Trail", "Altima"),
            "Opel" to listOf("Corsa", "Astra", "Insignia", "Mokka", "Zafira"),
            "Peugeot" to listOf("208", "308", "508", "2008", "3008", "5008"),
            "Renault" to listOf("Clio", "Megane", "Scenic", "Captur", "Kadjar", "Duster", "Logan"),
            "SEAT" to listOf("Ibiza", "Leon", "Ateca", "Arona"),
            "Škoda" to listOf("Fabia", "Octavia", "Superb", "Kamiq", "Karoq", "Kodiaq"),
            "Subaru" to listOf("Impreza", "Legacy", "Forester", "Outback", "XV"),
            "Suzuki" to listOf("Swift", "Vitara", "SX4", "Jimny"),
            "Toyota" to listOf("Yaris", "Corolla", "Camry", "Avensis", "RAV4", "Land Cruiser"),
            "Volkswagen" to
                listOf(
                    "Polo",
                    "Golf",
                    "Passat",
                    "Jetta",
                    "Tiguan",
                    "Touareg",
                    "Touran"
                ),
            "Volvo" to listOf("S60", "S80", "S90", "V60", "XC40", "XC60", "XC90")
        )
    val makes: List<String> get() = models.keys.toList()
    fun models(make: String): List<String> = models.entries.firstOrNull {
        normalized(it.key) == normalized(make)
    }?.value.orEmpty()
    fun normalized(value: String): String = java.text.Normalizer.normalize(
        value,
        java.text.Normalizer.Form.NFD
    )
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
    fun matches(name: String, query: String): Boolean {
        val normalizedQuery = normalized(query.trim())
        val aliases =
            when (normalized(name)) {
                "bmw" -> "бмв бмв"
                "volkswagen" -> "vw фольксваген фольксваґен"
                "skoda" -> "шкода"
                "toyota" -> "тойота тойота"
                "mercedes-benz" -> "мерседес mercedes benz"
                "renault" -> "рено"
                "peugeot" -> "пежо"
                "hyundai" -> "хюндай хендай хюндаї"
                "nissan" -> "ніссан ниссан"
                else -> ""
            }
        return normalizedQuery.isBlank() || normalized("$name $aliases").contains(normalizedQuery)
    }
}

@Singleton
class VehicleCatalogue @Inject constructor(@ApplicationContext context: Context) {
    private val directory = File(context.cacheDir, "vehicle-catalogue")
    suspend fun makes(): CatalogueResult = load(
        "makes",
        "GetMakesForVehicleType/car",
        VehicleSuggestions.makes,
        "MakeName",
        "Make_Name"
    )
    suspend fun models(make: String): CatalogueResult {
        if (make.isBlank()) return CatalogueResult(emptyList(), false)
        val name = URLEncoder.encode(make, StandardCharsets.UTF_8.name()).replace("+", "%20")
        return load(
            "models:$make",
            "GetModelsForMake/$name",
            VehicleSuggestions.models(make),
            "Model_Name",
            "ModelName"
        )
    }
    private suspend fun load(
        key: String,
        endpoint: String,
        fallback: List<String>,
        vararg fields: String
    ): CatalogueResult = withContext(Dispatchers.IO) {
        val file =
            File(
                directory,
                MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") {
                    "%02x".format(it)
                } +
                    ".json"
            )
        fun parse(raw: String): List<String> {
            val results = JSONObject(raw).getJSONArray("Results")
            require(results.length() <= 20_000)
            return (0 until results.length()).mapNotNull { index ->
                val row = results.getJSONObject(index)
                fields.firstNotNullOfOrNull { field ->
                    row.optString(field).takeIf {
                        it.isNotBlank() &&
                            it.length <= 100
                    }
                }
            }.distinctBy(VehicleSuggestions::normalized).sortedBy(VehicleSuggestions::normalized)
        }
        val cached =
            try {
                if (file.isFile &&
                    file.length() <= 2_000_000
                ) {
                    parse(file.readText())
                } else {
                    emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }
        if (cached.isNotEmpty() && System.currentTimeMillis() - file.lastModified() < 86_400_000) {
            return@withContext CatalogueResult(
                (fallback + cached).distinctBy(VehicleSuggestions::normalized),
                true
            )
        }
        val connection =
            URI(
                "https://vpic.nhtsa.dot.gov/api/vehicles/$endpoint?format=json"
            ).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("Accept", "application/json")
            require(connection.responseCode == 200)
            val raw =
                connection.inputStream.use { input ->
                    val bytes = input.readBytesWithLimit(2_000_000)
                    String(bytes, Charsets.UTF_8)
                }
            ensureActive()
            val names = parse(raw)
            if (names.isNotEmpty()) {
                directory.mkdirs()
                val temporary = File(directory, "${file.name}.${java.util.UUID.randomUUID()}.tmp")
                try {
                    temporary.writeText(raw)
                    if (!temporary.renameTo(file)) temporary.delete()
                } finally {
                    temporary.delete()
                }
            }
            CatalogueResult((fallback + names).distinctBy(VehicleSuggestions::normalized), true)
        } catch (
            cancel: CancellationException
        ) {
            throw cancel
        } catch (
            _: Exception
        ) {
            CatalogueResult((fallback + cached).distinctBy(VehicleSuggestions::normalized), false)
        } finally {
            connection.disconnect()
        }
    }
}

private fun java.io.InputStream.readBytesWithLimit(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= limit)
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
