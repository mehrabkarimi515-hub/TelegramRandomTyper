package com.example.telegramrandomtyper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("typer_data", Context.MODE_PRIVATE) }
    private val fields = mutableListOf<EditText>()
    private lateinit var delayField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        }
        scroll.addView(layout)

        layout.addView(TextView(this).apply {
            text = "تایپر تلگرام — نسخه اندروید"
            textSize = 22f
            gravity = Gravity.CENTER
        })
        layout.addView(TextView(this).apply {
            text = "متن‌ها را در ۸ کادر وارد کن. متن‌ها روی گوشی ذخیره می‌شوند."
            textSize = 14f
            setPadding(0, 12, 0, 12)
        })

        for (i in 0 until 8) {
            layout.addView(TextView(this).apply { text = "کادر ${i + 1}"; textSize = 16f })
            val edit = EditText(this).apply {
                minLines = 3
                maxLines = 5
                gravity = Gravity.TOP or Gravity.START
                hint = "متن کادر ${i + 1}"
                setText(prefs.getString("text_$i", ""))
            }
            fields.add(edit)
            layout.addView(edit)
        }

        layout.addView(TextView(this).apply {
            text = "مکث بین کلمه‌ها (میلی‌ثانیه) — مثل نسخه کامپیوتر"
            setPadding(0, 12, 0, 4)
        })
        delayField = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(prefs.getString("delay", "35"))
        }
        layout.addView(delayField)

        layout.addView(Button(this).apply {
            text = "ذخیره متن‌ها"
            setOnClickListener { saveData(); Toast.makeText(this@MainActivity, "ذخیره شد", Toast.LENGTH_SHORT).show() }
        })
        layout.addView(Button(this).apply {
            text = "۱) فعال‌کردن کیبورد تایپر"
            setOnClickListener { saveData(); startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        layout.addView(Button(this).apply {
            text = "۲) انتخاب کیبورد تایپر"
            setOnClickListener {
                saveData()
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        })
        layout.addView(TextView(this).apply {
            text = "روش استفاده: کیبورد تایپر را فعال و انتخاب کن؛ تلگرام را باز کن، وارد کادر پیام شو و از نوار تایپر دکمه شروع را بزن. ارسال پیام همچنان با خودت و دکمه ارسال تلگرام است."
            textSize = 14f
            setPadding(0, 16, 0, 16)
        })
        setContentView(scroll)
    }

    private fun saveData() {
        val edit = prefs.edit()
        fields.forEachIndexed { i, field -> edit.putString("text_$i", field.text.toString()) }
        edit.putString("delay", delayField.text.toString())
        edit.apply()
    }

    override fun onPause() {
        saveData()
        super.onPause()
    }
}
