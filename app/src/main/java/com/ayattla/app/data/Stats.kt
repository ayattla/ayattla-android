package com.ayattla.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Anonymous usage counts, to learn which tools and steps people use and where they stop.
 *
 * Privacy by design (see the privacy policy, «الإحصاءات المجهولة»):
 * - Only counters of fixed event names with fixed keys (a tool id, a step, an answer id), per day.
 * - No user or device identifier, no names, numbers, location, text or content of any kind.
 * - Counts are summed on the phone and sent in one small batch at most every few hours; the server
 *   adds them to daily totals and does not store IP addresses.
 * - On by default, with a clear switch in Settings (and shown at the end of the first-run questions);
 *   switching it off deletes anything not yet sent and stops counting.
 *
 * Same events as the iOS app (Stats.swift).
 */
object Stats {
    const val ENDPOINT = "https://ayattla.com/api/stats"
    private const val EVERY_MS = 6 * 3_600_000L
    private val safeKey = Regex("[^a-z0-9_:\\-]")

    private fun sp(c: Context) = c.getSharedPreferences("stats", Context.MODE_PRIVATE)

    fun enabled(c: Context) = sp(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, on: Boolean) {
        val e = sp(c).edit().putBoolean("enabled", on)
        if (!on) sp(c).all.keys.filter { it.startsWith("n|") }.forEach { e.remove(it) }
        e.apply()
    }

    /** Counts one event. [key] must come from the app's own ids (tool, step, choice) — never user text. */
    fun event(c: Context, name: String, key: String = "") {
        if (!enabled(c)) return
        val k = "n|${LocalDate.now()}|${clean(name)}|${clean(key)}"
        val p = sp(c)
        p.edit().putInt(k, p.getInt(k, 0) + 1).apply()
    }

    fun clean(s: String) = s.lowercase().replace(safeKey, "").take(48)

    /** The pending batch as sent: {"v":1,"platform":"android","version":"x","days":{"date":{"name|key":n}}}. */
    fun batch(c: Context, version: String): JSONObject? {
        val days = JSONObject()
        sp(c).all.forEach { (k, v) ->
            if (!k.startsWith("n|") || v !is Int) return@forEach
            val parts = k.split("|")
            if (parts.size != 4) return@forEach
            val day = days.optJSONObject(parts[1]) ?: JSONObject().also { days.put(parts[1], it) }
            day.put(parts[2] + "|" + parts[3], v)
        }
        if (days.length() == 0) return null
        return JSONObject().put("v", 1).put("platform", "android").put("version", version).put("days", days)
    }

    /** Sends what was counted (online, enabled, at most every 6 hours); cleared once the server accepts it. */
    suspend fun flush(c: Context, force: Boolean = false) = withContext(Dispatchers.IO) {
        val p = sp(c)
        if (!enabled(c) || !Net.isOnline) return@withContext
        if (!force && System.currentTimeMillis() - p.getLong("sent", 0) < EVERY_MS) return@withContext
        val version = runCatching { c.packageManager.getPackageInfo(c.packageName, 0).versionName }.getOrNull() ?: ""
        val body = batch(c, version) ?: return@withContext
        val sent = p.all.filter { it.key.startsWith("n|") && it.value is Int }.mapValues { it.value as Int }
        runCatching {
            val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000; conn.readTimeout = 10_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            if (ok) {
                // Subtract what was sent, so events counted while sending are kept for next time.
                val e = p.edit().putLong("sent", System.currentTimeMillis())
                sent.forEach { (k, n) -> val left = p.getInt(k, 0) - n; if (left > 0) e.putInt(k, left) else e.remove(k) }
                e.apply()
            }
        }
    }
}
