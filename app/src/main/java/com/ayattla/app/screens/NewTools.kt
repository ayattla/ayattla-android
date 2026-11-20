package com.ayattla.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ayattla.app.LocalContent
import com.ayattla.app.ui.components.AppCard
import com.ayattla.app.ui.components.Chip
import com.ayattla.app.ui.theme.Fonts
import com.ayattla.app.ui.theme.LocalBrand
import com.ayattla.app.ui.theme.Num

/** Pure sums for the expiations calculator (unit-tested). */
object Kaffarah {
    enum class Kind(val title: String) { Oath("كفارة اليمين"), Fidya("فدية الصيام"), Ramadan("كفارة الجماع في نهار رمضان") }
    /** Meals owed: ten poor per broken oath, one per day of fidya, sixty for the Ramadan expiation. */
    fun meals(kind: Kind, count: Int) = when (kind) { Kind.Oath -> 10 * count; Kind.Fidya -> count; Kind.Ramadan -> 60 * count }
}

@Composable
private fun NumberField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, { s -> onChange(s.filter { it.isDigit() || it == '.' }) }, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
    )
}

/** An ayah from the pack shown with its reference (the evidence under a calculator). */
@Composable
private fun Evidence(surah: Int, from: Int, to: Int) {
    val b = LocalBrand.current
    val content = LocalContent.current
    val ayat = remember { content.ayat(surah, from, to) }
    AppCard {
        Text(ayat.joinToString(" ") { it.text + " " + Num.ayahMark(it.ayah) }, fontFamily = Fonts.quran, fontSize = 19.sp, lineHeight = 36.sp,
            color = b.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("سورة ${content.surah(surah).shortName} · ${if (from == to) "$from" else "$from–$to"}", style = MaterialTheme.typography.labelMedium, color = b.primaryInk, modifier = Modifier.padding(top = 6.dp))
    }
}

/** «حاسبة الكفارات»: oath, fidya and the Ramadan expiation, as meals and money, with the evidence. */
@Composable
fun KaffarahScreen(nav: NavHostController) {
    val b = LocalBrand.current
    var kind by rememberSaveable { mutableStateOf(Kaffarah.Kind.Oath) }
    var count by rememberSaveable { mutableStateOf("1") }
    var meal by rememberSaveable { mutableStateOf("") }
    val n = count.toIntOrNull()?.coerceIn(0, 10000) ?: 0
    val price = meal.toDoubleOrNull() ?: 0.0
    val meals = Kaffarah.meals(kind, n)
    ToolPage(nav, "حاسبة الكفارات", "kaffarah") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Kaffarah.Kind.entries.forEach { k -> Chip(k.title, kind == k, { kind = k }) }
        }
        NumberField(count, when (kind) { Kaffarah.Kind.Oath -> "عدد الأيمان"; Kaffarah.Kind.Fidya -> "عدد الأيام"; Kaffarah.Kind.Ramadan -> "عدد الأيام" }) { count = it }
        NumberField(meal, "قيمة الوجبة الواحدة (اختياري)") { meal = it }
        AppCard {
            Text("المطلوب", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Text("إطعام $meals مسكينا", style = MaterialTheme.typography.headlineSmall, color = b.primaryInk)
            if (price > 0) Text("قرابة ${Num.money(meals * price)}", style = MaterialTheme.typography.titleMedium, color = b.goldInk)
            Spacer(Modifier.height(6.dp))
            Text(
                when (kind) {
                    Kaffarah.Kind.Oath -> "أو كسوة عشرة مساكين أو عتق رقبة، فمن لم يجد فصيام ثلاثة أيام عن كل يمين."
                    Kaffarah.Kind.Fidya -> "لمن لا يستطيع الصيام عجزا دائما كالكبير والمريض الذي لا يرجى برؤه: إطعام مسكين عن كل يوم."
                    Kaffarah.Kind.Ramadan -> "على الترتيب: عتق رقبة، فإن لم يجد فصيام شهرين متتابعين، فإن لم يستطع فإطعام ستين مسكينا، مع قضاء اليوم."
                },
                style = MaterialTheme.typography.bodyMedium, color = b.ink,
            )
        }
        Text("الدليل", style = MaterialTheme.typography.titleMedium, color = b.ink, modifier = Modifier.fillMaxWidth())
        when (kind) {
            Kaffarah.Kind.Oath -> Evidence(5, 89, 89)
            Kaffarah.Kind.Fidya -> Evidence(2, 184, 184)
            Kaffarah.Kind.Ramadan -> AppCard {
                Text("حديث أبي هريرة رضي الله عنه في الرجل الذي وقع على امرأته في رمضان", style = MaterialTheme.typography.bodyMedium, color = b.ink)
                Text("رواه البخاري (1936) ومسلم (1111)", style = MaterialTheme.typography.labelMedium, color = b.primaryInk, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Text("تقدير أولي للمساعدة، والحالات الخاصة يُسأل عنها أهل العلم.", style = MaterialTheme.typography.bodySmall, color = b.muted, modifier = Modifier.fillMaxWidth())
    }
}

/** «زكاة العملات الرقمية»: holdings × price, against the gold nisab; 2.5% when it is reached. */
@Composable
fun CryptoZakatScreen(nav: NavHostController) {
    val b = LocalBrand.current
    var rows by remember { mutableIntStateOf(2) }
    val amounts = remember { mutableStateOf(List(10) { "" }) }
    val prices = remember { mutableStateOf(List(10) { "" }) }
    var gold by rememberSaveable { mutableStateOf("") }
    var debts by rememberSaveable { mutableStateOf("") }
    val total = (0 until rows).sumOf { (amounts.value[it].toDoubleOrNull() ?: 0.0) * (prices.value[it].toDoubleOrNull() ?: 0.0) } - (debts.toDoubleOrNull() ?: 0.0)
    val nisab = (gold.toDoubleOrNull() ?: 0.0) * 85
    val reached = nisab > 0 && total >= nisab
    ToolPage(nav, "زكاة العملات الرقمية", "crypto-zakat") {
        AppCard {
            Text(if (reached) "الزكاة الواجبة" else if (nisab == 0.0) "أدخل سعر جرام الذهب لمعرفة النصاب" else "لم يبلغ المال النصاب", style = MaterialTheme.typography.titleMedium, color = b.ink)
            Text(Num.money(if (reached) total * 0.025 else 0.0), style = MaterialTheme.typography.headlineMedium, color = b.primaryInk)
            Text("القيمة الصافية ${Num.money(total.coerceAtLeast(0.0))} · النصاب ${Num.money(nisab)}", style = MaterialTheme.typography.bodySmall, color = b.muted)
        }
        NumberField(gold, "سعر جرام الذهب عيار 24 (النصاب 85 جراما)") { gold = it }
        (0 until rows).forEach { i ->
            Text("العملة ${i + 1}", style = MaterialTheme.typography.titleSmall, color = b.ink, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { NumberField(amounts.value[i], "الكمية") { s -> amounts.value = amounts.value.toMutableList().also { it[i] = s } } }
                androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { NumberField(prices.value[i], "السعر للوحدة") { s -> prices.value = prices.value.toMutableList().also { it[i] = s } } }
            }
        }
        if (rows < 10) Chip("أضف عملة", false, { rows++ })
        NumberField(debts, "ديون حالّة عليك (اختياري)") { debts = it }
        Text("تُحسب العملات الرقمية المملوكة للتجارة أو الادخار بقيمتها يوم وجوب الزكاة بعد حولان الحول، وفيها ربع العشر (2.5٪). تقدير أولي، والحالات الخاصة يُسأل عنها أهل العلم.",
            style = MaterialTheme.typography.bodySmall, color = b.muted, modifier = Modifier.fillMaxWidth())
    }
}
