package com.ayattla.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Quran reference used by topics and duas: surah, first and last ayah. */
data class QuranRef(val s: Int, val a: Int, val b: Int)

data class Topic(val slug: String, val title: String, val desc: String, val items: List<QuranRef>)

data class DuasData(val quran: List<QuranRef>, val needs: List<Need>) {
    data class Need(val need: String, val chapters: List<Chapter>)
    data class Chapter(val name: String, val items: List<String>)
}

data class PrayerGuide(val lead: String, val source: String, val sourceUrl: String, val sections: List<Section>) {
    data class Section(val title: String, val items: List<Item>)
    data class Item(val text: String, val source: String, val grade: String, val url: String)
}

/** Extra content stored as JSON in the pack's meta table (same keys as the iOS app). */
class Library(private val content: ContentStore) {

    val topics: List<Topic> by lazy {
        runCatching {
            val a = JSONArray(content.meta("topics") ?: "[]")
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                Topic(o.getString("slug"), o.getString("title"), o.optString("desc"), refs(o.getJSONArray("items")))
            }
        }.getOrDefault(emptyList())
    }

    val duas: DuasData? by lazy {
        runCatching {
            val o = JSONObject(content.meta("duas") ?: return@lazy null)
            val needs = o.getJSONArray("needs")
            DuasData(
                refs(o.optJSONArray("quran") ?: JSONArray()),
                List(needs.length()) { i ->
                    val n = needs.getJSONObject(i)
                    val ch = n.getJSONArray("chapters")
                    DuasData.Need(n.getString("need"), List(ch.length()) { j ->
                        val c = ch.getJSONObject(j)
                        val it = c.getJSONArray("items")
                        DuasData.Chapter(c.getString("name"), List(it.length()) { k -> it.getJSONObject(k).getString("text") })
                    })
                },
            )
        }.getOrNull()
    }

    val guide: PrayerGuide? by lazy {
        runCatching {
            val o = JSONObject(content.meta("prayer_guide") ?: return@lazy null)
            val secs = o.getJSONArray("sections")
            PrayerGuide(o.optString("lead"), o.optString("source"), o.optString("source_url"), List(secs.length()) { i ->
                val s = secs.getJSONObject(i)
                val items = s.getJSONArray("items")
                PrayerGuide.Section(s.getString("title"), List(items.length()) { j ->
                    val it = items.getJSONObject(j)
                    PrayerGuide.Item(it.getString("text"), it.optString("source"), it.optString("grade"), it.optString("url"))
                })
            })
        }.getOrNull()
    }

    private fun refs(a: JSONArray) = List(a.length()) { i ->
        val r = a.getJSONObject(i)
        QuranRef(r.getInt("s"), r.getInt("a"), r.optInt("b", r.getInt("a")))
    }
}
