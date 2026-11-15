package com.ayattla.app.crash

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * If the app crashes, the reason is saved and shown on a plain screen (no Compose, separate process)
 * with a button to send it, so a crash on any phone can be reported and fixed.
 */
object CrashReporter {
    fun install(context: Context) {
        val app = context.applicationContext ?: context
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val version = runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull()
                val report = "Ayat Tutla $version\nAndroid ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                    "${Build.MANUFACTURER} ${Build.MODEL}\nthread: ${thread.name}\n\n$sw"
                File(app.filesDir, "last_crash.txt").writeText(report)
                app.startActivity(Intent(app, CrashActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
            if (previous != null && thread.name != "main") previous.uncaughtException(thread, e)
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }
}

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = runCatching { File(filesDir, "last_crash.txt").readText() }.getOrDefault("")
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            setBackgroundColor(Color.parseColor("#F6F2E9"))
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        }
        root.addView(TextView(this).apply {
            text = "حدث خطأ أغلق التطبيق"
            textSize = 22f; setTextColor(Color.parseColor("#102D27")); gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "أرسل لنا التقرير بالزر أدناه لنصلحه. لا يحتوي التقرير على أي بيانات شخصية."
            textSize = 15f; setTextColor(Color.parseColor("#5C625D")); gravity = Gravity.CENTER
            setPadding(0, pad / 2, 0, pad)
        })
        root.addView(Button(this).apply {
            text = "إرسال التقرير"
            setOnClickListener {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "تقرير خطأ تطبيق آيات تتلى").putExtra(Intent.EXTRA_TEXT, report)
                startActivity(Intent.createChooser(send, "إرسال التقرير"))
            }
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(Button(this).apply {
            text = "إعادة فتح التطبيق"
            setOnClickListener {
                File(filesDir, "last_crash.txt").delete()
                packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) }
                finish()
            }
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(ScrollView(this).apply {
            addView(TextView(this@CrashActivity).apply {
                text = report; textSize = 11f; typeface = Typeface.MONOSPACE; setTextColor(Color.DKGRAY)
                textDirection = android.view.View.TEXT_DIRECTION_LTR; setTextIsSelectable(true)
                setPadding(0, pad, 0, 0)
            })
        }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)
    }
}
