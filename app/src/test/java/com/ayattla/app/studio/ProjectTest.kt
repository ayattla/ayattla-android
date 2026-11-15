package com.ayattla.app.studio

import com.ayattla.app.data.SiteMedia
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Studio projects survive a save and a reload exactly (they are the user's work). */
class ProjectTest {
    @Test fun projectRoundTrip() {
        val media = SiteMedia("pixabay", "128406", "video", "https://t/1.jpg", "https://p/1.mp4", 1080, 1920, true)
        val p = Project(
            name = "خواتيم البقرة", video = true, ratio = CardRatio.Story, quality = Quality.Q2160,
            ranges = listOf(AyahRange(2, 285, 286), AyahRange(112, 1, 4)),
            style = Style(
                colors = CardStyle.Night, text = TextLayer(1.2f, 0.7f, 0.1f, 0xFFD9C28E, false, -0.3f, Align.Start),
                ref = RefLayer(false, 1.3f, 0xFFFFFFFF, 0.5f), watermark = false, frame = false, veil = 0.6f,
                anim = Anim.Words, animMs = 900, wordPace = 0.5f,
            ),
            background = Background.Library(media), reciterId = "minshawi", pauses = mapOf("2:285" to 1500), perCard = 3,
        )
        val back = parseProject(JSONObject(p.json().toString()))
        assertEquals(p, back)
        assertEquals(6, back.ayahCount)
    }

    @Test fun defaultsWhenFieldsAreMissing() {
        val p = parseProject(JSONObject("""{"id":"x"}"""))
        assertEquals(Style(), p.style)
        assertEquals(Background.Color, p.background)
        assertEquals(listOf(AyahRange(1, 1, 7)), p.ranges)
    }

    @Test fun exportSizesAreEven() {
        for (r in CardRatio.entries) for (q in Quality.entries) {
            val (w, h) = Composer.size(r, q)
            assertTrue("$r $q", w % 2 == 0 && h % 2 == 0)
        }
        assertEquals(2160 to 3840, Composer.size(CardRatio.Story, Quality.Q2160))
        assertEquals(720 to 1280, Composer.size(CardRatio.Story, Quality.Q720))
    }

    @Test fun builtInTemplatesHaveUniqueNames() {
        assertEquals(TemplateStore.builtIn.size, TemplateStore.builtIn.map { it.name }.toSet().size)
    }
}
