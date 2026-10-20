package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.VideoFilter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * DynamicFilterManager: Controls dynamic video/camera filters completely from the Admin Panel.
 * Synchronizes with Firebase Firestore ('video_filters' collection) and local SharedPreferences.
 */
class DynamicFilterManager private constructor(context: Context? = null) {

    private val appContext: Context? = context?.applicationContext
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var filterListener: ListenerRegistration? = null
    private val prefs: SharedPreferences? by lazy {
        appContext?.getSharedPreferences("tashan_filters_prefs", Context.MODE_PRIVATE)
    }

    private val _filters = MutableStateFlow<List<VideoFilter>>(loadInitialFilters())
    val filters: StateFlow<List<VideoFilter>> = _filters.asStateFlow()

    companion object {
        private const val TAG = "DynamicFilterManager"
        private const val PREF_KEY_FILTERS = "admin_managed_filters_json"
        @Volatile
        private var INSTANCE: DynamicFilterManager? = null

        fun getInstance(context: Context? = null): DynamicFilterManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DynamicFilterManager(context).also { INSTANCE = it }
            }
        }

        fun getDefaultFilters(): List<VideoFilter> = listOf(
            VideoFilter(
                id = "filter_original",
                name = "Original",
                emoji = "✨",
                category = "Trending",
                colorTintHex = "#00000000",
                tintAlpha = 0.0f,
                saturation = 1.0f,
                contrast = 1.0f,
                brightness = 1.0f,
                badge = "",
                isActive = true,
                order = 0
            ),
            VideoFilter(
                id = "filter_portrait",
                name = "Portrait Glow",
                emoji = "🌸",
                category = "Portrait",
                colorTintHex = "#FFF0F5",
                tintAlpha = 0.12f,
                saturation = 1.1f,
                contrast = 1.05f,
                brightness = 1.08f,
                badge = "POPULAR",
                isActive = true,
                order = 1
            ),
            VideoFilter(
                id = "filter_cyberpunk",
                name = "Cyberpunk",
                emoji = "⚡",
                category = "Cyberpunk",
                colorTintHex = "#8B5CF6",
                tintAlpha = 0.22f,
                saturation = 1.45f,
                contrast = 1.25f,
                brightness = 0.98f,
                badge = "HOT",
                isActive = true,
                order = 2
            ),
            VideoFilter(
                id = "filter_golden_hour",
                name = "Golden Hour",
                emoji = "🌅",
                category = "Cinematic",
                colorTintHex = "#FFA500",
                tintAlpha = 0.18f,
                saturation = 1.25f,
                contrast = 1.12f,
                brightness = 1.05f,
                badge = "TRENDING",
                isActive = true,
                order = 3
            ),
            VideoFilter(
                id = "filter_vintage_90s",
                name = "Vintage 90s",
                emoji = "📼",
                category = "Retro",
                colorTintHex = "#795548",
                tintAlpha = 0.16f,
                saturation = 0.72f,
                contrast = 0.92f,
                brightness = 1.02f,
                badge = "CLASSIC",
                isActive = true,
                order = 4
            ),
            VideoFilter(
                id = "filter_noir_mono",
                name = "Noir Mono",
                emoji = "🖤",
                category = "Cinematic",
                colorTintHex = "#00000000",
                tintAlpha = 0.0f,
                saturation = 0.0f,
                contrast = 1.35f,
                brightness = 0.95f,
                badge = "",
                isActive = true,
                order = 5
            ),
            VideoFilter(
                id = "filter_neon_pop",
                name = "Neon Pop",
                emoji = "💖",
                category = "Trending",
                colorTintHex = "#EC4899",
                tintAlpha = 0.20f,
                saturation = 1.40f,
                contrast = 1.18f,
                brightness = 1.04f,
                badge = "VIRAL",
                isActive = true,
                order = 6
            ),
            VideoFilter(
                id = "filter_royal_gold",
                name = "Royal Gold",
                emoji = "👑",
                category = "Glamour",
                colorTintHex = "#EAB308",
                tintAlpha = 0.22f,
                saturation = 1.25f,
                contrast = 1.15f,
                brightness = 1.06f,
                badge = "VIP",
                isActive = true,
                order = 7
            ),
            VideoFilter(
                id = "filter_ai_beauty_ultra",
                name = "AI Beauty Ultra",
                emoji = "💎",
                category = "AI Effects",
                colorTintHex = "#FFFFFF",
                tintAlpha = 0.08f,
                saturation = 1.12f,
                contrast = 1.08f,
                brightness = 1.15f,
                badge = "AI",
                isActive = true,
                order = 8
            ),
            VideoFilter(
                id = "filter_ai_dream",
                name = "AI Dream",
                emoji = "🧠",
                category = "Effects",
                colorTintHex = "#E0B0FF",
                tintAlpha = 0.25f,
                saturation = 1.60f,
                contrast = 0.90f,
                brightness = 1.15f,
                badge = "AI EFFECT",
                isActive = true,
                order = 9
            ),
            VideoFilter(
                id = "filter_neon_pulse",
                name = "Neon Pulse",
                emoji = "🌀",
                category = "Effects",
                colorTintHex = "#00FFFF",
                tintAlpha = 0.20f,
                saturation = 1.80f,
                contrast = 1.20f,
                brightness = 1.10f,
                badge = "AI EFFECT",
                isActive = true,
                order = 10
            )
        )
    }

    init {
        attachFiltersListener()
    }

    private fun loadInitialFilters(): List<VideoFilter> {
        val savedJson = prefs?.getString(PREF_KEY_FILTERS, null)
        if (!savedJson.isNullOrBlank()) {
            try {
                val parsed = parseFiltersFromJson(savedJson)
                if (parsed.isNotEmpty()) return parsed
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing cached filters", e)
            }
        }
        return getDefaultFilters()
    }

    private fun saveFiltersToLocal(list: List<VideoFilter>) {
        try {
            val jsonArray = JSONArray()
            list.forEach { filter ->
                val obj = JSONObject().apply {
                    put("id", filter.id)
                    put("name", filter.name)
                    put("emoji", filter.emoji)
                    put("category", filter.category)
                    put("colorTintHex", filter.colorTintHex)
                    put("tintAlpha", filter.tintAlpha.toDouble())
                    put("saturation", filter.saturation.toDouble())
                    put("contrast", filter.contrast.toDouble())
                    put("brightness", filter.brightness.toDouble())
                    put("badge", filter.badge)
                    put("isActive", filter.isActive)
                    put("order", filter.order)
                    put("createdBy", filter.createdBy)
                    put("createdAt", filter.createdAt)
                }
                jsonArray.put(obj)
            }
            prefs?.edit()?.putString(PREF_KEY_FILTERS, jsonArray.toString())?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving filters to SharedPreferences", e)
        }
    }

    private fun parseFiltersFromJson(jsonStr: String): List<VideoFilter> {
        val list = mutableListOf<VideoFilter>()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                VideoFilter(
                    id = obj.optString("id", "filter_$i"),
                    name = obj.optString("name", "Custom"),
                    emoji = obj.optString("emoji", "✨"),
                    category = obj.optString("category", "Trending"),
                    colorTintHex = obj.optString("colorTintHex", "#00000000"),
                    tintAlpha = obj.optDouble("tintAlpha", 0.0).toFloat(),
                    saturation = obj.optDouble("saturation", 1.0).toFloat(),
                    contrast = obj.optDouble("contrast", 1.0).toFloat(),
                    brightness = obj.optDouble("brightness", 1.0).toFloat(),
                    badge = obj.optString("badge", ""),
                    isActive = obj.optBoolean("isActive", true),
                    order = obj.optInt("order", i),
                    createdBy = obj.optString("createdBy", "Admin"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        return list
    }

    fun attachFiltersListener() {
        try {
            filterListener?.remove()
            filterListener = firestore.collection("video_filters")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore video_filters listener warning: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val cloudFilters = snapshot.documents.mapNotNull { doc ->
                            try {
                                VideoFilter(
                                    id = doc.id,
                                    name = doc.getString("name") ?: "Filter",
                                    emoji = doc.getString("emoji") ?: "✨",
                                    category = doc.getString("category") ?: "Trending",
                                    colorTintHex = doc.getString("colorTintHex") ?: "#00000000",
                                    tintAlpha = (doc.getDouble("tintAlpha") ?: 0.0).toFloat(),
                                    saturation = (doc.getDouble("saturation") ?: 1.0).toFloat(),
                                    contrast = (doc.getDouble("contrast") ?: 1.0).toFloat(),
                                    brightness = (doc.getDouble("brightness") ?: 1.0).toFloat(),
                                    badge = doc.getString("badge") ?: "",
                                    isActive = doc.getBoolean("isActive") ?: true,
                                    order = (doc.getLong("order") ?: 0L).toInt(),
                                    createdBy = doc.getString("createdBy") ?: "Admin",
                                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedBy { it.order }
                        if (cloudFilters.isNotEmpty()) {
                            _filters.value = cloudFilters
                            saveFiltersToLocal(cloudFilters)
                            Log.d(TAG, "Loaded ${cloudFilters.size} filters live from Firestore")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching video_filters listener", e)
        }
    }

    /**
     * Admin creates and publishes a brand new filter directly from the Admin Panel.
     */
    fun addFilter(filter: VideoFilter, onResult: (Boolean, String) -> Unit) {
        val updated = _filters.value.toMutableList()
        val newFilterWithOrder = filter.copy(order = updated.size)
        updated.add(newFilterWithOrder)
        _filters.value = updated
        saveFiltersToLocal(updated)

        try {
            val data = hashMapOf(
                "name" to newFilterWithOrder.name,
                "emoji" to newFilterWithOrder.emoji,
                "category" to newFilterWithOrder.category,
                "colorTintHex" to newFilterWithOrder.colorTintHex,
                "tintAlpha" to newFilterWithOrder.tintAlpha.toDouble(),
                "saturation" to newFilterWithOrder.saturation.toDouble(),
                "contrast" to newFilterWithOrder.contrast.toDouble(),
                "brightness" to newFilterWithOrder.brightness.toDouble(),
                "badge" to newFilterWithOrder.badge,
                "isActive" to newFilterWithOrder.isActive,
                "order" to newFilterWithOrder.order,
                "createdBy" to newFilterWithOrder.createdBy,
                "createdAt" to newFilterWithOrder.createdAt
            )
            firestore.collection("video_filters").document(newFilterWithOrder.id)
                .set(data)
                .addOnSuccessListener {
                    onResult(true, "Filter '${newFilterWithOrder.name}' successfully published to app!")
                }
                .addOnFailureListener { e ->
                    onResult(true, "Filter '${newFilterWithOrder.name}' saved locally & active!")
                }
        } catch (e: Exception) {
            onResult(true, "Filter '${newFilterWithOrder.name}' active!")
        }
    }

    fun updateFilter(filter: VideoFilter, onResult: (Boolean, String) -> Unit) {
        val updated = _filters.value.map {
            if (it.id == filter.id) filter else it
        }
        _filters.value = updated
        saveFiltersToLocal(updated)

        try {
            val data = hashMapOf<String, Any>(
                "name" to filter.name,
                "emoji" to filter.emoji,
                "category" to filter.category,
                "colorTintHex" to filter.colorTintHex,
                "tintAlpha" to filter.tintAlpha.toDouble(),
                "saturation" to filter.saturation.toDouble(),
                "contrast" to filter.contrast.toDouble(),
                "brightness" to filter.brightness.toDouble(),
                "badge" to filter.badge,
                "isActive" to filter.isActive,
                "order" to filter.order
            )
            firestore.collection("video_filters").document(filter.id)
                .update(data)
                .addOnSuccessListener {
                    onResult(true, "Filter '${filter.name}' updated successfully!")
                }
                .addOnFailureListener {
                    onResult(true, "Filter '${filter.name}' updated locally!")
                }
        } catch (e: Exception) {
            onResult(true, "Filter '${filter.name}' updated!")
        }
    }

    fun toggleFilterActive(filterId: String, isActive: Boolean, onResult: (Boolean, String) -> Unit) {
        val target = _filters.value.find { it.id == filterId }
        val updated = _filters.value.map {
            if (it.id == filterId) it.copy(isActive = isActive) else it
        }
        _filters.value = updated
        saveFiltersToLocal(updated)

        try {
            firestore.collection("video_filters").document(filterId)
                .update("isActive", isActive)
                .addOnCompleteListener {
                    val statusStr = if (isActive) "enabled" else "disabled"
                    onResult(true, "Filter '${target?.name ?: ""}' is now $statusStr for users.")
                }
        } catch (e: Exception) {
            onResult(true, "Filter status updated.")
        }
    }

    fun deleteFilter(filterId: String, onResult: (Boolean, String) -> Unit) {
        val target = _filters.value.find { it.id == filterId }
        val updated = _filters.value.filter { it.id != filterId }
        _filters.value = updated
        saveFiltersToLocal(updated)

        try {
            firestore.collection("video_filters").document(filterId)
                .delete()
                .addOnCompleteListener {
                    onResult(true, "Filter '${target?.name ?: ""}' removed.")
                }
        } catch (e: Exception) {
            onResult(true, "Filter removed.")
        }
    }

    fun resetToDefaults(onResult: (Boolean, String) -> Unit) {
        val defaults = getDefaultFilters()
        _filters.value = defaults
        saveFiltersToLocal(defaults)

        try {
            defaults.forEach { f ->
                firestore.collection("video_filters").document(f.id).set(f)
            }
            onResult(true, "Filters reset to standard defaults.")
        } catch (e: Exception) {
            onResult(true, "Filters reset to standard defaults.")
        }
    }

    /**
     * Fetches live visual effects from Pixabay API and converts them into active VideoFilters
     * so they appear directly in the Camera and Video Editor filter & effect trays.
     */
    suspend fun syncLivePixabayEffects(query: String = "neon particles"): List<VideoFilter> {
        return try {
            val pixabayEffects = com.example.data.pixabay.PixabayClient.getInstance()
                .searchVisualEffects(query = query)
            val converted = pixabayEffects.take(12).mapIndexed { index, fx ->
                VideoFilter(
                    id = fx.id,
                    name = fx.title,
                    emoji = "🎬",
                    category = fx.category,
                    colorTintHex = fx.tintColorHex,
                    tintAlpha = fx.blendAlpha,
                    saturation = 1.28f,
                    contrast = 1.15f,
                    brightness = 1.05f,
                    badge = "PIXABAY FX",
                    isActive = true,
                    order = 20 + index,
                    createdBy = fx.creatorName
                )
            }
            if (converted.isNotEmpty()) {
                val base = _filters.value.filterNot { it.id.startsWith("px_fx_") }
                val merged = base + converted
                _filters.value = merged
                saveFiltersToLocal(merged)
            }
            _filters.value
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing live Pixabay effects", e)
            _filters.value
        }
    }
}

