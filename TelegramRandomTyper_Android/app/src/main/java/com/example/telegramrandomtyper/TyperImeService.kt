package com.example.telegramrandomtyper

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Random
import kotlin.math.min

class TyperImeService : InputMethodService() {
    @Volatile private var running = false
    @Volatile private var worker: Thread? = null
    private lateinit var status: TextView

    override fun onCreateInputView(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 10)
            setBackgroundColor(Color.rgb(235, 238, 242))
        }
        status = TextView(this).apply {
            text = "تایپر آماده است"
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            textSize = 14f
        }
        panel.addView(status)
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(Button(this).apply {
            text = "شروع / ادامه"
            setOnClickListener { startTyping() }
        }, LinearLayout.LayoutParams(0, 54, 1f))
        buttons.addView(Button(this).apply {
            text = "توقف"
            setOnClickListener { running = false; setStatus("متوقف شد") }
        }, LinearLayout.LayoutParams(0, 54, 1f))
        panel.addView(buttons)
        panel.addView(TextView(this).apply {
            text = "متن‌ها را در برنامه تایپر تنظیم کن؛ ارسال پیام با خودت است."
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            textSize = 12f
        })
        return panel
    }

    private fun startTyping() {
        if (running) return
        val prefs = getSharedPreferences("typer_data", MODE_PRIVATE)
        val original = (0 until 8).map { prefs.getString("text_$it", "") ?: "" }
        if (original.all { it.isBlank() }) {
            setStatus("اول متن‌ها را در برنامه وارد و ذخیره کن")
            return
        }
        val delayMs = (prefs.getString("delay", "35") ?: "35")
            .replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 2000.0) ?: 35.0
        running = true
        worker = Thread {
            val rng = Random()
            val pools = original.map { splitKeepingWhitespace(it).toMutableList() }.toMutableList()
            val originals = original.map { splitKeepingWhitespace(it) }
            var active = -1
            var last = -1

            while (running) {
                var available = pools.indices.filter { pools[it].isNotEmpty() }
                if (available.isEmpty()) {
                    for (i in pools.indices) {
                        pools[i].clear()
                        pools[i].addAll(originals[i])
                    }
                    available = pools.indices.filter { pools[it].isNotEmpty() }
                    active = -1
                    if (available.isEmpty()) break
                    setStatus("دور جدید")
                }
                if (active !in available) {
                    var choices = available.filter { it != last }
                    if (choices.isEmpty()) choices = available
                    active = choices[rng.nextInt(choices.size)]
                    last = active
                    setStatus("در حال تایپ کادر ${active + 1}")
                }

                val chunk = pools[active].removeAt(0)
                var typed = 0
                for (ch in chunk) {
                    if (!running) break
                    val connection = currentInputConnection ?: break
                    if (!connection.commitText(ch.toString(), 1)) break
                    typed++
                    val pause = when {
                        ch in ".!?،؛,:：؟" -> 160L + rng.nextInt(121)
                        ch.isWhitespace() -> 8L + rng.nextInt(13)
                        else -> {
                            var p = 45L + rng.nextInt(61)
                            if (rng.nextInt(100) < 8) p += 60L + rng.nextInt(71)
                            p
                        }
                    }
                    try { Thread.sleep(pause) } catch (_: InterruptedException) { break }
                }
                if (typed < chunk.length) {
                    pools[active].add(0, chunk.substring(typed))
                    continue
                }
                try { Thread.sleep((delayMs + rng.nextInt(16)).toLong()) } catch (_: InterruptedException) { break }

                if (pools[active].isEmpty() && pools.indices.any { it != active && pools[it].isNotEmpty() }) {
                    setStatus("کادر تمام شد؛ ۵ ثانیه مکث")
                    var waited = 0
                    while (waited < 5000 && running) {
                        try { Thread.sleep(50) } catch (_: InterruptedException) { break }
                        waited += 50
                    }
                }
            }
            running = false
            setStatus("پایان دور؛ برای شروع دوباره بزن")
        }.also { it.start() }
    }

    private fun splitKeepingWhitespace(text: String): List<String> {
        val result = mutableListOf<String>()
        val regex = Regex("\\\\s+|[^\\\\s]+")
        regex.findAll(text).forEach { result.add(it.value) }
        return result
    }

    private fun setStatus(message: String) {
        runOnUiThread { if (::status.isInitialized) status.text = message }
    }

    override fun onFinishInput() {
        running = false
        super.onFinishInput()
    }

    override fun onDestroy() {
        running = false
        worker?.interrupt()
        super.onDestroy()
    }
}
