package it.autoago

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("auto_ago", MODE_PRIVATE) }
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.ITALY)
    private lateinit var content: LinearLayout
    private lateinit var dateField: TextView
    private lateinit var kmField: EditText
    private lateinit var fuelField: Spinner
    private lateinit var priceField: EditText
    private lateinit var amountField: EditText
    private lateinit var notesField: EditText

    private val red = Color.rgb(214, 45, 52)
    private val dark = Color.rgb(35, 38, 45)
    private val muted = Color.rgb(103, 109, 121)
    private val page = Color.rgb(247, 248, 251)

    override fun onCreate(state: Bundle?) { super.onCreate(state); showEntry() }

    private fun showEntry() {
        val body = baseScreen("Nuovo rifornimento", "Inserisci i dati del rifornimento")
        val card = card()
        body.addView(card)

        card.addView(sectionTitle("DATA E ORA"))
        dateField = TextView(this).apply {
            text = dateFormat.format(Date()); textSize = 17f; setTextColor(dark)
            gravity = Gravity.CENTER_VERTICAL; setPadding(18, 0, 18, 0); background = fieldBackground()
        }
        card.addView(dateField, fieldParams(52))

        card.addView(sectionTitle("DATI DEL RIFORNIMENTO"))
        kmField = input("KM letti dal cruscotto", false); card.addView(kmField, fieldParams(56))
        fuelField = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, arrayOf("Metano (met)", "Benzina (benz)", "Gasolio (Gas)"))
            background = fieldBackground(); setPadding(12, 0, 12, 0)
        }
        card.addView(fuelField, fieldParams(56))
        priceField = input("Prezzo al litro  €", true); card.addView(priceField, fieldParams(56))
        amountField = input("Importo pagato  €", true); card.addView(amountField, fieldParams(56))

        card.addView(sectionTitle("NOTE / MANUTENZIONI"))
        notesField = input("Scrivi qui eventuali dettagli", false).apply { minLines = 3; gravity = Gravity.TOP; setPadding(18, 14, 18, 14) }
        card.addView(notesField, fieldParams(92))

        val save = Button(this).apply {
            text = "  SALVA RIFORNIMENTO"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); background = rounded(red, 18f); isAllCaps = false
            setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0)
            compoundDrawablePadding = 10; setOnClickListener { saveFuel() }
        }
        body.addView(save, LinearLayout.LayoutParams(-1, 58).apply { setMargins(0, 18, 0, 12) })
        val hint = TextView(this).apply { text = "I dati vengono conservati anche se chiudi l'app"; textSize = 12f; setTextColor(muted); gravity = Gravity.CENTER }
        body.addView(hint, LinearLayout.LayoutParams(-1, 30))
    }

    private fun showArchive() {
        val body = baseScreen("Archivio rifornimenti", "I più recenti sono visualizzati per primi")
        val records = recordsSorted()
        if (records.isEmpty()) {
            body.addView(TextView(this).apply { text = "Nessun rifornimento registrato"; textSize = 18f; setTextColor(muted); gravity = Gravity.CENTER; setPadding(0, 50, 0, 50) })
        } else {
            val scroll = HorizontalScrollView(this)
            val table = TableLayout(this).apply { setPadding(0, 4, 0, 4) }
            table.addView(row(arrayOf("DATA", "KM", "TIPO", "€/L", "EURO", "KM/€", "NOTE"), true))
            var previousKm: Double? = null
            records.forEach { r ->
                val km = r.optDouble("km"); val amount = r.optDouble("amount")
                val perEuro = previousKm?.let { (km - it) / amount }
                table.addView(row(arrayOf(r.optString("date"), fmt(km), shortFuel(r.optString("fuel")), fmt(r.optDouble("price"), 3), fmt(amount, 2), perEuro?.let { fmt(it, 2) } ?: "—", r.optString("notes").ifBlank { "—" }), false))
                previousKm = km
            }
            scroll.addView(table); body.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        val export = Button(this).apply {
            text = "ESPORTA / SALVA FILE TXT"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; isAllCaps = false
            setTextColor(red); background = rounded(Color.WHITE, 16f); setOnClickListener { chooseExportLocation() }
        }
        body.addView(export, LinearLayout.LayoutParams(-1, 54).apply { setMargins(0, 14, 0, 8) })
        val info = TextView(this).apply { text = "Il file viene salvato inizialmente nella memoria privata dell'app. Usa il pulsante sopra per scegliere Download o un'altra cartella."; textSize = 12f; setTextColor(muted); setPadding(8, 0, 8, 8) }
        body.addView(info)
    }

    private fun baseScreen(title: String, subtitle: String): LinearLayout {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(page); setPadding(18, 12, 18, 12) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, 4) }
        val logo = ImageView(this).apply { setImageResource(R.drawable.app_icon); contentDescription = "Auto Ago" }
        top.addView(logo, LinearLayout.LayoutParams(58, 58))
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 0, 0, 0) }
        titles.addView(TextView(this).apply { text = "AUTO AGO"; textSize = 24f; typeface = Typeface.DEFAULT_BOLD; setTextColor(dark) })
        titles.addView(TextView(this).apply { text = title; textSize = 15f; setTextColor(red) })
        top.addView(titles, LinearLayout.LayoutParams(0, -2, 1f)); root.addView(top)
        root.addView(TextView(this).apply { text = subtitle; textSize = 13f; setTextColor(muted); setPadding(70, 0, 0, 10) }, LinearLayout.LayoutParams(-1, 34))
        val nav = LinearLayout(this).apply { setPadding(0, 0, 0, 10) }
        nav.addView(navButton("＋  Nuovo") { showEntry() }, LinearLayout.LayoutParams(0, 46, 1f))
        nav.addView(navButton("▤  Archivio") { showArchive() }, LinearLayout.LayoutParams(0, 46, 1f))
        root.addView(nav)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root); return content
    }

    private fun saveFuel() {
        val km = number(kmField); val price = number(priceField); val amount = number(amountField)
        if (km == null || price == null || amount == null || km < 0 || price < 0 || amount <= 0) { toast("Controlla KM, prezzo e importo"); return }
        val all = readRecords(); all.put(JSONObject().apply { put("date", dateField.text); put("timestamp", System.currentTimeMillis()); put("km", km); put("fuel", fuelField.selectedItem); put("price", price); put("amount", amount); put("notes", notesField.text.toString()) })
        prefs.edit().putString("records", all.toString()).apply(); writeInternalTxt(all); toast("Rifornimento salvato"); showArchive()
    }

    private fun chooseExportLocation() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "text/plain"; putExtra(Intent.EXTRA_TITLE, "rifornimenti.txt"); addCategory(Intent.CATEGORY_OPENABLE) }
        startActivityForResult(intent, 42)
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        if (request == 42 && result == RESULT_OK && data?.data != null) try { contentResolver.openOutputStream(data.data!!)?.use { it.write(exportText(readRecords()).toByteArray(Charsets.UTF_8)) }; toast("File TXT salvato nella cartella scelta") } catch (_: Exception) { toast("Impossibile salvare il file") }
    }

    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 8, 16, 16); background = rounded(Color.WHITE, 22f); elevation = 5f }
    private fun sectionTitle(s: String) = TextView(this).apply { text = s; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; setTextColor(red); setPadding(2, 18, 2, 8) }
    private fun input(hint: String, decimal: Boolean) = EditText(this).apply { this.hint = hint; textSize = 16f; setTextColor(dark); setHintTextColor(muted); background = fieldBackground(); inputType = if (decimal) 8194 else 2; setPadding(18, 0, 18, 0) }
    private fun fieldParams(h: Int) = LinearLayout.LayoutParams(-1, h).apply { setMargins(0, 4, 0, 7) }
    private fun fieldBackground() = rounded(Color.rgb(250, 250, 252), 12f, Color.rgb(225, 227, 233), 2)
    private fun rounded(color: Int, radius: Float, stroke: Int? = null, width: Int = 0) = GradientDrawable().apply { setColor(color); cornerRadius = radius; if (stroke != null) setStroke(width, stroke) }
    private fun navButton(text: String, action: () -> Unit) = Button(this).apply { this.text = text; textSize = 13f; isAllCaps = false; setTextColor(dark); background = rounded(Color.WHITE, 14f, Color.rgb(230, 231, 235), 1); setOnClickListener { action() } }
    private fun row(values: Array<String>, header: Boolean) = TableRow(this).apply { values.forEach { value -> addView(TextView(this@MainActivity).apply { text = value; textSize = if (header) 11f else 12f; typeface = if (header) Typeface.DEFAULT_BOLD else Typeface.DEFAULT; setTextColor(if (header) Color.WHITE else dark); setPadding(12, 14, 12, 14); background = rounded(if (header) red else Color.WHITE, 0f) }) } }
    private fun readRecords() = try { JSONArray(prefs.getString("records", "[]")) } catch (_: Exception) { JSONArray() }
    private fun recordsSorted() = (0 until readRecords().length()).map { readRecords().getJSONObject(it) }.sortedByDescending { it.optLong("timestamp") }
    private fun number(e: EditText) = e.text.toString().trim().replace(',', '.').toDoubleOrNull()
    private fun fmt(n: Double, decimals: Int = 0) = String.format(Locale.ITALY, "%.${decimals}f", n)
    private fun shortFuel(s: String) = when { s.startsWith("Metano") -> "met"; s.startsWith("Benzina") -> "benz"; else -> "Gas" }
    private fun txtFile() = File(filesDir, "rifornimenti.txt")
    private fun exportText(records: JSONArray): String { val sorted = (0 until records.length()).map { records.getJSONObject(it) }.sortedByDescending { it.optLong("timestamp") }; return buildString { appendLine("Data\tKM\tCarburante\tPrezzo/L\tImporto\tKM per €\tNote"); var previous: Double? = null; sorted.forEach { r -> val km = r.optDouble("km"); val amount = r.optDouble("amount"); appendLine("${r.optString("date")}\t${fmt(km)}\t${r.optString("fuel")}\t${fmt(r.optDouble("price"), 3)}\t${fmt(amount, 2)}\t${previous?.let { fmt((km - it) / amount, 2) } ?: "—"}\t${r.optString("notes").replace('\t', ' ')}"); previous = km } } }
    private fun writeInternalTxt(records: JSONArray) { txtFile().writeText(exportText(records), Charsets.UTF_8) }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
