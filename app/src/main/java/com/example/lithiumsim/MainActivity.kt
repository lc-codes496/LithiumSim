package com.example.lithiumsim

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var tvTitle: TextView
    private lateinit var tvSub: TextView
    private lateinit var tvCode: TextView
    private lateinit var tvVersion: TextView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // full screen moderno
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        hideSystemBars()

        setContentView(R.layout.activity_main)

        tvTitle = findViewById(R.id.tv_title)
        tvSub = findViewById(R.id.tv_sub)
        tvCode = findViewById(R.id.tv_code)
        tvVersion = findViewById(R.id.tv_version)

        tts = TextToSpeech(this, this)

        handler.postDelayed({ startSequence() }, 700)
    }

    private fun hideSystemBars() {
        try {
            window.decorView.windowInsetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } catch (e: Exception) {
            // fallback: ignore
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // tenta setar locale en-US
            val desired = Locale("en", "US")
            val res = tts.setLanguage(desired)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.getDefault()
            }

            // Voz fria: pitch mais baixo, velocidade um pouco mais lenta
            tts.setPitch(0.7f)
            tts.setSpeechRate(0.85f)

            // tenta selecionar uma Voice en_US disponível para soarem mais "robóticas" / masculinas
            try {
                val voices: Set<Voice>? = tts.voices
                if (voices != null) {
                    val chosen = voices.firstOrNull { v ->
                        val lc = v.locale
                        (lc != null && lc.language == "en" && lc.country == "US")
                    } ?: voices.firstOrNull()
                    chosen?.let { tts.voice = it }
                }
            } catch (e: Exception) {
                // ignora se API não suportar ou falhar
            }
        }
    }

    private fun startSequence() {
        tvCode.text = "LITHIUM DETECTED"
        tvTitle.text = "Shutdown now!"
        tvSub.text = "No command"
        tvVersion.text = "Android Go"

        // Mensagens em inglês — sem dizer "simulation"
        val messages = listOf(
            "This device has a critical battery failure. Lithium detected. Dispose of the device safely.",
            "All normal system features will be disabled.",
            "Initiating shutdown sequence."
        )

        var delay = 0L
        for (msg in messages) {
            handler.postDelayed({
                tvSub.text = msg
                tts.speak(msg, TextToSpeech.QUEUE_ADD, null, msg.hashCode().toString())
            }, delay)
            delay += 4200L
        }

        // Efeito final: muda texto para "Power off" e encerra o app (não desliga)
        handler.postDelayed({
            tvTitle.text = "Power off"
            tvSub.text = "System will power down"
            handler.postDelayed({
                finishAffinity()
            }, 1500)
        }, delay + 300)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        handler.removeCallbacksAndMessages(null)
    }
}
