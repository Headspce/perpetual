package com.progranimator.perpetual

import android.content.Context
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads cartridges. Order of preference:
 *  1. cached cartridge in internal storage (instant, works offline)
 *  2. bundled fallback in assets/cartridge_default (first launch)
 * Updates are polled from the Google Drive manifest URL; a newer
 * version number triggers an asset download + hot swap.
 */
object CartridgeLoader {
    private const val TAG = "Perpetual"

    private fun rootDir(context: Context): File =
        File(context.filesDir, "cartridge").apply { mkdirs() }

    fun assetDir(context: Context): File =
        File(rootDir(context), "assets").apply { mkdirs() }

    fun cachedCartridge(context: Context): Cartridge? {
        val f = File(rootDir(context), "current.json")
        if (f.exists()) {
            try { return Cartridge.parse(f.readText()) } catch (e: Exception) {
                Log.w(TAG, "cached manifest unreadable", e)
            }
        }
        return installBundled(context)
    }

    private fun installBundled(context: Context): Cartridge? {
        return try {
            val dir = assetDir(context)
            context.assets.list("cartridge_default")?.forEach { name ->
                context.assets.open("cartridge_default/$name").use { inp ->
                    File(dir, name).outputStream().use { inp.copyTo(it) }
                }
            }
            val json = context.assets.open("cartridge_default/cartridge.json")
                .bufferedReader().readText()
            File(rootDir(context), "current.json").writeText(json)
            Cartridge.parse(json)
        } catch (e: Exception) {
            Log.w(TAG, "bundled cartridge failed", e)
            null
        }
    }

    /**
     * Checks the Drive manifest for a newer cartridge version.
     * Returns true when a newer cartridge was installed.
     * Must be called off the main thread.
     */
    fun checkForUpdate(context: Context, manifestUrl: String): Boolean {
        return try {
            val json = httpGet(manifestUrl) ?: return false
            val remote = Cartridge.parse(json)
            val cur = cachedCartridge(context)
            if (cur != null && remote.version <= cur.version) return false

            val stage = File(rootDir(context), "staging").apply {
                mkdirs(); listFiles()?.forEach { it.delete() }
            }
            remote.files.forEach { (name, id) ->
                val data = httpGetBytes(
                    "https://drive.google.com/uc?export=download&id=$id"
                ) ?: return@forEach
                File(stage, name).writeBytes(data)
            }
            File(stage, "cartridge.json").writeText(json)

            val assets = assetDir(context)
            assets.listFiles()?.forEach { it.delete() }
            stage.listFiles()?.forEach { it.renameTo(File(assets, it.name)) }
            stage.delete()
            File(rootDir(context), "current.json").writeText(json)
            Log.i(TAG, "cartridge updated to v${remote.version} (${remote.name})")
            true
        } catch (e: Exception) {
            Log.w(TAG, "cartridge update failed", e)
            false
        }
    }

    private fun httpGet(url: String): String? =
        httpGetBytes(url)?.toString(Charsets.UTF_8)

    private fun httpGetBytes(url: String): ByteArray? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Perpetual/1.0")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { it.readBytes() }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
