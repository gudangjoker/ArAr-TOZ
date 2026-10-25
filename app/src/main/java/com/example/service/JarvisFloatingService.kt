package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ai.download.AiModelDownloadManager
import com.example.ai.engine.JarvisAiEngine
import com.example.data.AppDatabase
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

class JarvisFloatingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager

    private var floatingBubbleView: View? = null
    private var popupDialogView: View? = null

    private lateinit var aiEngine: JarvisAiEngine
    private lateinit var repository: FinanceRepository
    private lateinit var downloadManager: AiModelDownloadManager

    private var speechRecognizer: SpeechRecognizer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val db = AppDatabase.getDatabase(applicationContext)
        repository = FinanceRepository(db.financeDao())
        downloadManager = AiModelDownloadManager(applicationContext)
        aiEngine = JarvisAiEngine(applicationContext, repository, downloadManager)

        // Try initializing LLM in background if available
        serviceScope.launch(Dispatchers.IO) {
            aiEngine.initializeLlm()
        }

        startForegroundNotification()

        if (Settings.canDrawOverlays(this)) {
            createFloatingBubble()
        } else {
            Toast.makeText(this, "Izin Overlay (Tampil di atas aplikasi lain) diperlukan untuk Asisten Cici", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun startForegroundNotification() {
        val channelId = "channel_jarvis_floating"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Asisten Melayang Cici",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Layanan gelembung melayang Cici untuk input perintah belanja di mana saja"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Asisten Si Cici Aktif 🌸")
            .setContentText("Ketuk gelembung melayang untuk mencatat belanja & tagihan kapan saja.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingBubble() {
        if (floatingBubbleView != null) return

        val bubbleSize = dpToPx(56)

        val bubble = ImageView(this).apply {
            setImageResource(R.drawable.ic_arar_toz_logo)
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#00695C"))
                setStroke(dpToPx(2), Color.WHITE)
            }
            background = bg
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            elevation = dpToPx(8).toFloat()
            contentDescription = "Tombol Melayang Si Cici"
        }

        val layoutParams = WindowManager.LayoutParams(
            bubbleSize,
            bubbleSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isClick = false
                    }
                    layoutParams.x = initialX + dx
                    layoutParams.y = initialY + dy
                    windowManager.updateViewLayout(bubble, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        showMiniAssistantDialog(layoutParams.x, layoutParams.y)
                    }
                    true
                }
                else -> false
            }
        }

        floatingBubbleView = bubble
        windowManager.addView(bubble, layoutParams)
    }

    private fun showMiniAssistantDialog(anchorX: Int, anchorY: Int) {
        if (popupDialogView != null) {
            dismissMiniDialog()
            return
        }

        val context = this
        val dialogWidth = dpToPx(320)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                setColor(Color.WHITE)
                setStroke(dpToPx(1), Color.parseColor("#C7EBE6"))
            }
            background = bg
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(16))
            elevation = dpToPx(16).toFloat()
        }

        // Header: Logo + Title + Close Button
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = ImageView(context).apply {
            setImageResource(R.drawable.ic_arar_toz_logo)
            val size = dpToPx(28)
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = dpToPx(8) }
        }

        val title = TextView(context).apply {
            text = "Asisten Si Cici 🌸"
            setTextColor(Color.parseColor("#00695C"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.GRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener { dismissMiniDialog() }
        }

        header.addView(logo)
        header.addView(title)
        header.addView(closeBtn)
        root.addView(header)

        // Subtitle instructions
        val subtitle = TextView(context).apply {
            text = "Ketik atau ucapkan: \"Beli telur 28rb\", \"Bayar listrik\", \"Wishlist mesin cuci\""
            setTextColor(Color.parseColor("#555555"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, dpToPx(6), 0, dpToPx(10))
        }
        root.addView(subtitle)

        // Input Row: EditText + Mic Button + Send Button
        val inputRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val inputEdit = EditText(context).apply {
            hint = "Perintah belanja / kas..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            val editBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#F4F7F4"))
            }
            background = editBg
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Voice Command Button
        val voiceBtn = Button(context).apply {
            text = "🎤"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#C7EBE6"))
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(44)).apply {
                marginStart = dpToPx(6)
            }
            setOnClickListener {
                startVoiceListening(inputEdit)
            }
        }

        // Send Button
        val sendBtn = Button(context).apply {
            text = "Kirim"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#00695C"))
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(dpToPx(60), dpToPx(44)).apply {
                marginStart = dpToPx(6)
            }
            setOnClickListener {
                val query = inputEdit.text.toString().trim()
                if (query.isNotEmpty()) {
                    executeCommandFromOverlay(query)
                }
            }
        }

        inputRow.addView(inputEdit)
        inputRow.addView(voiceBtn)
        inputRow.addView(sendBtn)
        root.addView(inputRow)

        // Status Feedback View
        val statusText = TextView(context).apply {
            text = "💡 Siap mendengarkan perintah offline"
            setTextColor(Color.parseColor("#00695C"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, dpToPx(8), 0, 0)
        }
        root.addView(statusText)

        val params = WindowManager.LayoutParams(
            dialogWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (anchorX - dpToPx(120)).coerceAtLeast(dpToPx(16))
            y = anchorY + dpToPx(64)
        }

        popupDialogView = root
        windowManager.addView(root, params)

        // Focus keyboard
        inputEdit.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(inputEdit, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun executeCommandFromOverlay(command: String) {
        serviceScope.launch {
            try {
                Toast.makeText(applicationContext, "Memproses: \"$command\"...", Toast.LENGTH_SHORT).show()
                val response = withContext(Dispatchers.IO) {
                    aiEngine.processUserCommand(command)
                }

                // Show confirmation toast
                Toast.makeText(
                    applicationContext,
                    response.reply,
                    Toast.LENGTH_LONG
                ).show()

                dismissMiniDialog()
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startVoiceListening(targetEditText: EditText) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition tidak tersedia di perangkat ini", Toast.LENGTH_SHORT).show()
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    targetEditText.hint = "Mendengarkan... Silakan bicara"
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    targetEditText.hint = "Memproses suara..."
                }
                override fun onError(error: Int) {
                    targetEditText.hint = "Gagal menangkap suara (Error: $error)"
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        targetEditText.setText(spokenText)
                        executeCommandFromOverlay(spokenText)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "id-ID")
        }
        speechRecognizer?.startListening(intent)
    }

    private fun dismissMiniDialog() {
        popupDialogView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            popupDialogView = null
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissMiniDialog()
        floatingBubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            floatingBubbleView = null
        }
        speechRecognizer?.destroy()
        speechRecognizer = null
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 2002
        private const val PREFS_KEY_FLOATING_ENABLED = "key_floating_service_enabled"

        fun isFloatingServiceEnabled(context: Context): Boolean {
            return context.getSharedPreferences("jarvis_ai_prefs", Context.MODE_PRIVATE)
                .getBoolean(PREFS_KEY_FLOATING_ENABLED, false)
        }

        fun setFloatingServiceEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences("jarvis_ai_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean(PREFS_KEY_FLOATING_ENABLED, enabled).apply()

            val intent = Intent(context, JarvisFloatingService::class.java)
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.stopService(intent)
            }
        }
    }
}
