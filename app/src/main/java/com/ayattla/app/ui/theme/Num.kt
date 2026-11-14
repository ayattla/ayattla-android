package com.ayattla.app.ui.theme

/** Numbers in the interface use English digits (owner's rule for ayattla.com). */
object Num {
    private val arabicIndic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /** Arabic-Indic digits, used only inside the Quran end-of-ayah mark. */
    fun ayahMark(n: Int): String = "۝" + n.toString().map { arabicIndic[it - '0'] }.joinToString("")
    fun quranNumber(n: Int): String = n.toString().map { arabicIndic[it - '0'] }.joinToString("")

    fun duration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "$m دقيقة"
            m == 0 -> "$h ساعة"
            else -> "$h ساعة و$m دقيقة"
        }
    }

    fun money(x: Double): String = String.format(java.util.Locale.US, "%,.2f", x)
}
