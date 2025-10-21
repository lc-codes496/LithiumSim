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
import kotlin.random.Random

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var tvTitle: TextView
    private lateinit var tvSub: TextView
    private lateinit var tvCode: TextView
    private lateinit var tvVersion: TextView
    private lateinit var tvSection: TextView
    private val handler = Handler(Looper.getMainLooper())

    // Lista de "seções" simuladas — nomes genéricos e seguros (sem dados sensíveis)
    private val sections = listOf(
        "Battery / Power Management",
        "Thermal Zone",
        "Charging Circuit",
        "Bootloader",
        "Recovery Partition",
        "Wi‑Fi Module",
        "Bluetooth Module",
        "Cellular Radio",
        "Storage Controller",
        "Sensor Hub",
        "Camera Subsystem",
        "Audio DSP",
        "Display Driver",
        "GPS Module",
        "Security Enclave"
    )

    private fun pickRandomSection(): Pair<String,String> {
        val name = sections[Random.nextInt(sections.size)]
        // status aleatório curto para efeito dramático
        val statuses = listOf("Critical", "Fail", "Unresponsive", "Overheated", "Leaking")
        val status = statuses[Random.nextInt(statuses.size)]
        return Pair(name, status)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // full screen (compatível com APIs modernas)
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
        tvSection = findViewById(R.id.tv_section)

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
            // fallback: ignore em versões mais antigas
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val desired = Locale("en", "US")
            val res = tts.setLanguage(desired)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.getDefault()
            }

            // Voz fria
            tts.setPitch(0.65f)
            tts.setSpeechRate(0.78f)

            // tenta selecionar uma voice en_US disponível
            try {
                val voices: Set<Voice>? = tts.voices
                if (!voices.isNullOrEmpty()) {
                    val chosen = voices.firstOrNull { v ->
                        val lc = v.locale
                        (lc != null && lc.language == "en" && lc.country == "US")
                    } ?: voices.firstOrNull()
                    chosen?.let { tts.voice = it }
                }
            } catch (e: Exception) {
                // ignora se falhar
            }
        }
    }

    private fun startSequence() {
        tvCode.text = "LITHIUM DETECTED"
        tvTitle.text = "Shutdown now!"
        tvSub.text = "No command"
        tvVersion.text = "Android Go"
        tvSection.text = "" // limpa antes

        // Mensagens principais em EN
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

            // a cada mensagem, também fala uma seção aleatória (para simular relatório)
            handler.postDelayed({
                val (secName, secStatus) = pickRandomSection()
                val sectionText = "Section: $secName — Status: $secStatus."
                tvSection.text = sectionText
                // fala a seção em tom curto e "frio"
                tts.speak(sectionText, TextToSpeech.QUEUE_ADD, null, sectionText.hashCode().toString())
            }, delay - 800) // fala a seção um pouco antes da próxima mensagem
        }

        // Efeito final: muda texto para "Power off" e encerra o app (não desliga)
        handler.postDelayed({
            tvTitle.text = "Power off"
            tvSub.text = "System will power down"
            // mostra mais uma seção final antes de fechar
            val (secName, secStatus) = pickRandomSection()
            val finalSection = "Final Section: $secName — Status: $secStatus."
            tvSection.text = finalSection
            tts.speak(finalSection, TextToSpeech.QUEUE_ADD, null, finalSection.hashCode().toString())

            handler.postDelayed({
                finishAffinity()
            }, 1700)
        }, delay + 300)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        handler.removeCallbacksAndMessages(null)
    }
}
