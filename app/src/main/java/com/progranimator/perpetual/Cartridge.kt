package com.progranimator.perpetual

import android.graphics.Color
import org.json.JSONObject

/**
 * A cartridge describes everything around the frozen progress bar:
 * background, tabs, item grid, labels and colors. The bar's own
 * graphics are drawn by fixed code and never change.
 */
data class GridItem(val icon: String, val label: String)

data class Cartridge(
    val name: String,
    val version: Int,
    val title: String,
    val tabs: List<String>,
    val activeTab: Int,
    val barLabel: String,
    val progress: Float,
    val progressLabel: String,
    val backgroundColor: Int,
    val backgroundImage: String?,
    val accent: Int,
    val hint: String,
    val columns: Int,
    val grid: List<GridItem>,
    val files: Map<String, String>,
) {
    companion object {
        fun parse(json: String): Cartridge {
            val o = JSONObject(json)
            val grid = o.getJSONObject("grid")
            val items = grid.getJSONArray("items")
            val filesObj = o.getJSONObject("files")
            val files = mutableMapOf<String, String>()
            filesObj.keys().forEach { k -> files[k] = filesObj.getString(k) }
            val tabs = o.getJSONArray("tabs")
            return Cartridge(
                name = o.getString("cartridge"),
                version = o.getInt("version"),
                title = o.optString("title", ""),
                tabs = List(tabs.length()) { tabs.getString(it) },
                activeTab = o.optInt("activeTab", 0),
                barLabel = o.optString("barLabel", "LOADING"),
                progress = o.optDouble("progress", 0.0).toFloat(),
                progressLabel = o.optString("progressLabel", ""),
                backgroundColor = parseColor(o.optString("backgroundColor", "#d9c895")),
                backgroundImage = o.optString("backgroundImage", null),
                accent = parseColor(o.optString("accent", "#e33d2e")),
                hint = o.optString("hint", ""),
                columns = grid.optInt("columns", 4),
                grid = List(items.length()) {
                    val it = items.getJSONObject(it)
                    GridItem(it.getString("icon"), it.getString("label"))
                },
                files = files,
            )
        }

        private fun parseColor(s: String): Int =
            try { Color.parseColor(s) } catch (_: Exception) { Color.GRAY }
    }
}
