package com.example.ai.engine

import android.content.Context
import com.example.ai.download.AiModelDownloadManager
import com.example.data.entity.BillEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.WishlistEntity
import com.example.data.model.DefaultExpenseCategories
import com.example.data.model.DefaultIncomeCategories
import com.example.data.model.TransactionTypes
import com.example.data.model.WalletSources
import com.example.data.model.WishlistCategories
import com.example.data.model.WishlistPriorities
import com.example.data.repository.FinanceRepository
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.MathExpressionEvaluator
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class JarvisAiEngine(
    private val context: Context,
    private val repository: FinanceRepository,
    private val downloadManager: AiModelDownloadManager
) {
    private val gson = Gson()
    private var llmInference: LlmInference? = null
    private var loadedModelPath: String? = null

    /**
     * Initializes MediaPipe LiteRT / Tasks-GenAI LlmInference if model file exists.
     */
    suspend fun initializeLlm(): Boolean = withContext(Dispatchers.IO) {
        val modelFile = downloadManager.getActiveModelFile() ?: return@withContext false
        if (!modelFile.exists() || modelFile.length() <= 0) return@withContext false

        if (llmInference != null && loadedModelPath == modelFile.absolutePath) {
            return@withContext true
        }

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(512)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            loadedModelPath = modelFile.absolutePath
            true
        } catch (e: Exception) {
            e.printStackTrace()
            llmInference = null
            false
        }
    }

    fun isLlmLoaded(): Boolean = llmInference != null

    /**
     * Main conversation interface.
     * Takes user message, generates structured JSON via Gemma or fallback NLU,
     * parses the action, executes it to FinanceRepository, and returns response.
     */
    suspend fun processUserCommand(userInput: String): JarvisResponse = withContext(Dispatchers.IO) {
        val cleanInput = userInput.trim()
        if (cleanInput.isEmpty()) {
            return@withContext JarvisResponse(
                action = JarvisActionType.CHAT,
                reply = "Ada yang bisa Cici bantu catat hari ini, Bunda? 🌸"
            )
        }

        // Try LLM if loaded
        var rawResponse: String? = null
        if (llmInference != null) {
            try {
                val prompt = buildGemmaPrompt(cleanInput)
                rawResponse = llmInference?.generateResponse(prompt)
            } catch (e: Exception) {
                e.printStackTrace()
                rawResponse = null
            }
        }

        // Parse structured response
        val parsedResponse = if (!rawResponse.isNullOrBlank()) {
            parseStructuredJson(rawResponse) ?: ruleBasedFallbackParser(cleanInput)
        } else {
            ruleBasedFallbackParser(cleanInput)
        }

        // Execute action directly to FinanceRepository
        executeAction(parsedResponse)
    }

    private fun buildGemmaPrompt(userInput: String): String {
        return """
            <start_of_turn>system
            Kamu adalah Cici, asisten pintar offline pencatatan keuangan dan belanja rumah tangga keluarga ArAr TOZ.
            Tugasmu mengekstrak instruksi pengguna ke format JSON terstruktur untuk operasi database keuangan.
            Kategori pengeluaran: Belanja Dapur & Sayur, Belanja Bulanan & Supermarket, Jajan Anak & Sekolah, Listrik, Air & Gas, Kebutuhan Rumah Tangga, Kesehatan & Skincare, Sedekah & Sosial, Hiburan & Rekreasi, Lain-lain.
            Sumber dompet: Dompet Tunai, Rekening Bank, e-Wallet.

            Format JSON WAJIB:
            {
              "action": "ADD_TRANSACTION" | "ADD_WISHLIST" | "PAY_BILL" | "GET_SUMMARY" | "CHAT",
              "params": {
                 // Untuk ADD_TRANSACTION: "amount": number, "category": string, "type": "EXPENSE"|"INCOME", "walletSource": string, "note": string
                 // Untuk ADD_WISHLIST: "name": string, "targetAmount": number, "isSavingGoal": boolean, "categoryType": string, "priority": "Tinggi"|"Sedang"|"Rendah", "note": string
                 // Untuk PAY_BILL: "title": string, "amount": number, "walletSource": string
              },
              "reply": "Pesan respons ramah untuk pengguna"
            }
            Hanya keluarkan valid JSON tanpa teks tambahan.
            <end_of_turn>
            <start_of_turn>user
            $userInput
            <end_of_turn>
            <start_of_turn>model
        """.trimIndent()
    }

    private fun parseStructuredJson(rawText: String): JarvisResponse? {
        return try {
            val jsonStart = rawText.indexOf('{')
            val jsonEnd = rawText.lastIndexOf('}')
            if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) return null

            val jsonString = rawText.substring(jsonStart, jsonEnd + 1)
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject

            val actionStr = jsonObject.get("action")?.asString ?: "CHAT"
            val actionType = try {
                JarvisActionType.valueOf(actionStr.uppercase())
            } catch (_: Exception) {
                JarvisActionType.CHAT
            }

            val params = jsonObject.getAsJsonObject("params") ?: JsonObject()
            val reply = jsonObject.get("reply")?.asString ?: "Siap, perintah sudah diproses."

            JarvisResponse(
                action = actionType,
                params = params,
                reply = reply,
                rawJson = jsonString
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Offline NLU rule-based fallback parser for high reliability
     * even before multi-gigabyte LLM model weights are installed.
     */
    private suspend fun ruleBasedFallbackParser(input: String): JarvisResponse {
        val lower = input.lowercase()

        // 1. GET_SUMMARY check
        if (lower.contains("ringkasan") || lower.contains("saldo") || lower.contains("rekap") ||
            lower.contains("laporan") || lower.contains("sisa kas") || lower.contains("berapa pengeluaran")
        ) {
            return JarvisResponse(
                action = JarvisActionType.GET_SUMMARY,
                reply = "Baik Bunda, ini ringkasan kas keuangan keluarga ArAr TOZ saat ini:"
            )
        }

        // 2. PAY_BILL check
        if (lower.contains("bayar tagihan") || lower.contains("bayar listrik") || lower.contains("bayar wifi") ||
            lower.contains("bayar air") || lower.contains("bayar bpjs") || lower.contains("lunas tagihan")
        ) {
            val amount = extractAmount(lower)
            val title = when {
                lower.contains("listrik") || lower.contains("pln") -> "Listrik PLN"
                lower.contains("wifi") || lower.contains("indihome") || lower.contains("internet") -> "Wi-Fi"
                lower.contains("air") || lower.contains("pdam") -> "Air PDAM"
                lower.contains("bpjs") -> "BPJS"
                else -> "Tagihan Rutin"
            }
            val wallet = extractWallet(lower)

            val params = JsonObject().apply {
                addProperty("title", title)
                addProperty("amount", amount)
                addProperty("walletSource", wallet)
            }
            return JarvisResponse(
                action = JarvisActionType.PAY_BILL,
                params = params,
                reply = "Siap Bunda, tagihan $title akan ditandai lunas dan dicatat ke pengeluaran."
            )
        }

        // 3. ADD_WISHLIST check
        if (lower.contains("wishlist") || lower.contains("daftar belanja") || lower.contains("beli nanti") ||
            lower.contains("target tabungan") || lower.contains("mau nabung") || lower.contains("ingat belanja")
        ) {
            val amount = extractAmount(lower)
            val itemName = extractWishlistName(lower)
            val isSavingGoal = lower.contains("target") || lower.contains("nabung") || lower.contains("impian") || amount >= 500_000L

            val params = JsonObject().apply {
                addProperty("name", itemName)
                addProperty("targetAmount", amount)
                addProperty("isSavingGoal", isSavingGoal)
                addProperty("categoryType", if (isSavingGoal) WishlistCategories.HOUSEHOLD else WishlistCategories.KITCHEN)
                addProperty("priority", WishlistPriorities.HIGH)
                addProperty("note", "Dicatat otomatis via Cici")
            }
            return JarvisResponse(
                action = JarvisActionType.ADD_WISHLIST,
                params = params,
                reply = "Siap Bunda! '$itemName' ${if (amount > 0) "dengan target ${CurrencyFormatter.formatRupiah(amount)}" else ""} sudah dimasukkan ke ${if (isSavingGoal) "target tabungan" else "daftar belanja"}."
            )
        }

        // 4. ADD_TRANSACTION check
        val amount = extractAmount(lower)
        if (amount > 0 || lower.contains("beli") || lower.contains("catat") || lower.contains("keluar") || lower.contains("gaji")) {
            val isIncome = lower.contains("gaji") || lower.contains("nafkah") || lower.contains("pemasukan") ||
                    lower.contains("bonus") || lower.contains("dapat uang") || lower.contains("hasil usaha")
            val type = if (isIncome) TransactionTypes.INCOME else TransactionTypes.EXPENSE
            val category = if (isIncome) {
                when {
                    lower.contains("usaha") || lower.contains("jualan") -> DefaultIncomeCategories.USAHA
                    lower.contains("bonus") || lower.contains("thr") -> DefaultIncomeCategories.BONUS
                    else -> DefaultIncomeCategories.NAFKAH
                }
            } else {
                extractExpenseCategory(lower)
            }
            val wallet = extractWallet(lower)
            val note = extractNote(input)

            val params = JsonObject().apply {
                addProperty("type", type)
                addProperty("amount", amount)
                addProperty("category", category)
                addProperty("walletSource", wallet)
                addProperty("note", note)
            }

            val prefixMsg = if (isIncome) "Alhamdulillah! Pemasukan" else "Siap Bunda! Pengeluaran"
            return JarvisResponse(
                action = JarvisActionType.ADD_TRANSACTION,
                params = params,
                reply = "$prefixMsg ${CurrencyFormatter.formatRupiah(amount)} untuk $category berhasil dicatat ($wallet)."
            )
        }

        // 5. Default CHAT
        return JarvisResponse(
            action = JarvisActionType.CHAT,
            reply = "Halo Bunda! Saya Cici, asisten pintar offline belanja keluarga Anda. Bunda bisa bilang:\n• 'Catat beli sayur 25.000'\n• 'Beli susu anak 85rb pakai Bank'\n• 'Masukkan mesin cuci 2jt ke wishlist'\n• 'Bayar tagihan listrik'\n• 'Berapa saldo kas saya?'"
        )
    }

    private suspend fun executeAction(response: JarvisResponse): JarvisResponse {
        return try {
            when (response.action) {
                JarvisActionType.ADD_TRANSACTION -> {
                    val p = response.params
                    val type = p.get("type")?.asString ?: TransactionTypes.EXPENSE
                    val amount = p.get("amount")?.asLong ?: 0L
                    val category = p.get("category")?.asString ?: DefaultExpenseCategories.DAPUR
                    val wallet = p.get("walletSource")?.asString ?: WalletSources.CASH
                    val note = p.get("note")?.asString ?: "Via Si Cici"

                    if (amount > 0) {
                        val tx = TransactionEntity(
                            type = type,
                            amount = amount,
                            category = category,
                            walletSource = wallet,
                            dateTimestamp = DateTimeUtils.getCurrentJakartaTimeMillis(),
                            note = note
                        )
                        val id = repository.insertTransaction(tx)
                        response.copy(
                            executedSuccessfully = true,
                            executionDetails = "Transaksi #$id berhasil disimpan: ${if (type == TransactionTypes.EXPENSE) "- " else "+ "}${CurrencyFormatter.formatRupiah(amount)} ($category)"
                        )
                    } else {
                        response.copy(
                            executedSuccessfully = false,
                            reply = "Nominal transaksi belum terdeteksi. Silakan sebutkan nominalnya, misal: 'Beli telur 28.000'."
                        )
                    }
                }

                JarvisActionType.ADD_WISHLIST -> {
                    val p = response.params
                    val name = p.get("name")?.asString ?: "Barang Belanja"
                    val target = p.get("targetAmount")?.asLong ?: 0L
                    val isSaving = p.get("isSavingGoal")?.asBoolean ?: false
                    val catType = p.get("categoryType")?.asString ?: WishlistCategories.KITCHEN
                    val prio = p.get("priority")?.asString ?: WishlistPriorities.MEDIUM
                    val note = p.get("note")?.asString ?: "Dicatat oleh Si Cici"

                    val item = WishlistEntity(
                        name = name,
                        targetAmount = target,
                        isSavingGoal = isSaving,
                        categoryType = catType,
                        priority = prio,
                        note = note,
                        createdAt = System.currentTimeMillis()
                    )
                    val id = repository.insertWishlist(item)
                    response.copy(
                        executedSuccessfully = true,
                        executionDetails = "Item #$id '$name' berhasil ditambahkan ke ${if (isSaving) "target tabungan" else "daftar belanja"}!"
                    )
                }

                JarvisActionType.PAY_BILL -> {
                    val p = response.params
                    val targetTitle = p.get("title")?.asString ?: "Listrik PLN"
                    val paidAmount = p.get("amount")?.asLong ?: 0L
                    val wallet = p.get("walletSource")?.asString ?: WalletSources.BANK

                    val allBills = repository.allBills.first()
                    val matchingBill = allBills.find {
                        it.title.contains(targetTitle, ignoreCase = true) || targetTitle.contains(it.title, ignoreCase = true)
                    }

                    if (matchingBill != null) {
                        val finalAmount = if (paidAmount > 0) paidAmount else matchingBill.estimatedAmount
                        val currentMonthKey = DateTimeUtils.getCurrentMonthYearKey()
                        repository.updateBill(
                            matchingBill.copy(
                                lastPaidMonthYear = currentMonthKey,
                                walletSource = wallet
                            )
                        )
                        repository.insertTransaction(
                            TransactionEntity(
                                type = TransactionTypes.EXPENSE,
                                amount = finalAmount,
                                category = matchingBill.category,
                                walletSource = wallet,
                                dateTimestamp = DateTimeUtils.getCurrentJakartaTimeMillis(),
                                note = "Bayar Tagihan: ${matchingBill.title} (Via Si Cici)"
                            )
                        )
                        response.copy(
                            executedSuccessfully = true,
                            reply = "Tagihan '${matchingBill.title}' lunas bulan ini dan pengeluaran ${CurrencyFormatter.formatRupiah(finalAmount)} sudah dicatat!",
                            executionDetails = "Tagihan '${matchingBill.title}' lunas & dicatat ke kas."
                        )
                    } else {
                        // Create transaction anyway
                        val finalAmount = if (paidAmount > 0) paidAmount else 100_000L
                        repository.insertTransaction(
                            TransactionEntity(
                                type = TransactionTypes.EXPENSE,
                                amount = finalAmount,
                                category = DefaultExpenseCategories.LISTRIK,
                                walletSource = wallet,
                                dateTimestamp = DateTimeUtils.getCurrentJakartaTimeMillis(),
                                note = "Bayar: $targetTitle (Via Si Cici)"
                            )
                        )
                        response.copy(
                            executedSuccessfully = true,
                            reply = "Pengeluaran bayar $targetTitle sebesar ${CurrencyFormatter.formatRupiah(finalAmount)} sudah berhasil dicatat!",
                            executionDetails = "Pengeluaran $targetTitle tercatat: ${CurrencyFormatter.formatRupiah(finalAmount)}"
                        )
                    }
                }

                JarvisActionType.GET_SUMMARY -> {
                    val summary = repository.getQuickFinancialSummary()
                    val netBal = summary["netBalance"] as? Long ?: 0L
                    val todayExp = summary["todayExpense"] as? Long ?: 0L
                    val monthExp = summary["monthExpense"] as? Long ?: 0L
                    val monthInc = summary["monthIncome"] as? Long ?: 0L
                    val unpaidCount = summary["unpaidBillsCount"] as? Int ?: 0

                    val replyMsg = buildString {
                        append("📊 Ringkasan Kas ArAr TOZ:\n")
                        append("• Total Sisa Kas: ${CurrencyFormatter.formatRupiah(netBal)}\n")
                        append("• Belanja Hari Ini: ${CurrencyFormatter.formatRupiah(todayExp)}\n")
                        append("• Belanja Bulan Ini: ${CurrencyFormatter.formatRupiah(monthExp)}\n")
                        append("• Pemasukan Bulan Ini: ${CurrencyFormatter.formatRupiah(monthInc)}\n")
                        if (unpaidCount > 0) {
                            append("• Tagihan Belum Dibayar: $unpaidCount tagihan")
                        } else {
                            append("• Semua tagihan bulan ini sudah beres! ✨")
                        }
                    }

                    response.copy(
                        executedSuccessfully = true,
                        reply = replyMsg,
                        executionDetails = "Ringkasan keuangan diambil dari Room Database"
                    )
                }

                JarvisActionType.CHAT -> {
                    response.copy(executedSuccessfully = true)
                }
            }
        } catch (e: Exception) {
            response.copy(
                executedSuccessfully = false,
                reply = "Maaf Bunda, terjadi kendala saat mencatat: ${e.message}"
            )
        }
    }

    // --- Helper NLP extractors ---

    private fun extractAmount(text: String): Long {
        // Match numbers with 'k', 'rb', 'ribu', 'jt', 'juta'
        val regexK = Regex("""(\d+(?:[.,]\d+)?)\s*(?:rb|ribu|k)""", RegexOption.IGNORE_CASE)
        val matchK = regexK.find(text)
        if (matchK != null) {
            val num = matchK.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
            return (num * 1000).toLong()
        }

        val regexJt = Regex("""(\d+(?:[.,]\d+)?)\s*(?:jt|juta)""", RegexOption.IGNORE_CASE)
        val matchJt = regexJt.find(text)
        if (matchJt != null) {
            val num = matchJt.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
            return (num * 1_000_000).toLong()
        }

        // Match "rp 25.000" or raw numbers
        val regexRp = Regex("""(?:rp\.?|sebesar)?\s*(\d{1,3}(?:[.]\d{3})+|\d+)""", RegexOption.IGNORE_CASE)
        val matchRp = regexRp.find(text)
        if (matchRp != null) {
            val cleaned = matchRp.groupValues[1].replace(".", "")
            return cleaned.toLongOrNull() ?: 0L
        }

        return MathExpressionEvaluator.evaluateMathExpression(text)
    }

    private fun extractExpenseCategory(text: String): String {
        return when {
            text.contains("sayur") || text.contains("dapur") || text.contains("bumbu") ||
                    text.contains("ikan") || text.contains("ayam") || text.contains("daging") ||
                    text.contains("cabe") || text.contains("bawang") || text.contains("telur") -> DefaultExpenseCategories.DAPUR

            text.contains("supermarket") || text.contains("sabun") || text.contains("sampo") ||
                    text.contains("deterjen") || text.contains("minyak") || text.contains("beras") -> DefaultExpenseCategories.BULANAN

            text.contains("jajan") || text.contains("anak") || text.contains("sekolah") ||
                    text.contains("popok") || text.contains("susu") || text.contains("buku") -> DefaultExpenseCategories.ANAK

            text.contains("listrik") || text.contains("token") || text.contains("air") ||
                    text.contains("pdam") || text.contains("gas") || text.contains("elpiji") -> DefaultExpenseCategories.LISTRIK

            text.contains("obat") || text.contains("dokter") || text.contains("klinik") ||
                    text.contains("skincare") || text.contains("vitamin") -> DefaultExpenseCategories.KESEHATAN

            text.contains("sedekah") || text.contains("infaq") || text.contains("zakat") ||
                    text.contains("masjid") || text.contains("sumbangan") -> DefaultExpenseCategories.SEDEKAH

            text.contains("kopi") || text.contains("cafe") || text.contains("nonton") ||
                    text.contains("jalan") || text.contains("liburan") -> DefaultExpenseCategories.HIBURAN

            text.contains("rumah") || text.contains("galon") || text.contains("sapu") -> DefaultExpenseCategories.RUMAH

            else -> DefaultExpenseCategories.LAINNYA
        }
    }

    private fun extractWallet(text: String): String {
        return when {
            text.contains("bank") || text.contains("transfer") || text.contains("bca") ||
                    text.contains("bri") || text.contains("mandiri") || text.contains("bni") -> WalletSources.BANK
            text.contains("gopay") || text.contains("ovo") || text.contains("dana") ||
                    text.contains("shopeepay") || text.contains("ewallet") || text.contains("qris") -> WalletSources.EWALLET
            else -> WalletSources.CASH
        }
    }

    private fun extractWishlistName(text: String): String {
        var clean = text
            .replace("tambah", "")
            .replace("tambahkan", "")
            .replace("wishlist", "")
            .replace("ke", "")
            .replace("daftar belanja", "")
            .replace("target", "")
            .replace("tabungan", "")
            .replace("beli", "")
            .replace("mau", "")
            .trim()

        // Strip amounts from name
        clean = clean.replace(Regex("""(?:rp\.?|sebesar)?\s*\d+.*"""), "").trim()
        if (clean.isBlank()) clean = "Barang Belanja"
        return clean.capitalizeWord()
    }

    private fun extractNote(text: String): String {
        return text.replace(Regex("""(?i)\b(catat|beli|masukkan|bayar|tambah)\b"""), "").trim().capitalizeWord()
    }

    private fun String.capitalizeWord(): String {
        return this.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
