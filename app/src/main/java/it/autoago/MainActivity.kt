package it.autoago

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("auto_ago", MODE_PRIVATE) }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ITALY)
    private lateinit var content: LinearLayout
    private lateinit var dateField: TextView
    private lateinit var kmField: EditText
    private lateinit var fuelField: Spinner
    private lateinit var priceField: EditText
    private lateinit var amountField: EditText
    private lateinit var notesField: EditText

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        showEntry()
    }

    private fun base(title: String): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 12)
            setBackgroundColor(0xFFF7F9FC.toInt())
        }
        val header = TextView(this).apply {
            text = title
            textSize = 24f
            setTextColor(0xFF0D47A1.toInt())
            setPadding(4, 4, 4, 14)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(header)
        val nav = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, 0, 0, 12) }
        nav.addView(button("Nuovo rifornimento") { showEntry() }, LinearLayout.LayoutParams(0, 48, 1f))
        nav.addView(button("Archivio") { showArchive() }, LinearLayout.LayoutParams(0, 48, 1f))
        root.addView(nav)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        return content
    }

    private fun showEntry() {
        val body = base("Auto Ago · Rifornimento")
        dateField = TextView(this).apply {
            text = dateFormat.format(Date())
            textSize = 18f
            setTextColor(0xFF263238.toInt())
            setPadding(14, 14, 14, 14)
            setBackgroundColor(0xFFE3F2FD.toInt())
        }
        body.addView(label("Data e ora (automatiche)")); body.addView(dateField)
        kmField = edit("KM letti dal cruscotto", false, "es. 125430")
        body.addView(kmField)
        body.addView(label("Carburante"))
        fuelField = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, arrayOf("Metano (met)", "Benzina (benz)", "Gasolio (Gas)")) }
        body.addView(fuelField)
        priceField = edit("Prezzo al litro (€)", true, "es. 1,789")
        body.addView(priceField)
        amountField = edit("Importo pagato (€)", true, "es. 35,00")
        body.addView(amountField)
        notesField = edit("Note / manutenzioni", false, "Dettagli facoltativi")
        notesField.minLines = 3
        body.addView(notesField)

        val save = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_save)
            contentDescription = "Salva rifornimento"
            setBackgroundColor(0xFF1565C0.toInt())
            setColorFilter(0xFFFFFFFF.toInt())
            setOnClickListener { saveFuel() }
        }
        body.addView(save, LinearLayout.LayoutParams(-1, 58).apply { setMargins(0, 18, 0, 8) })
        Toast.makeText(this, "Compila i dati e premi l'icona floppy", Toast.LENGTH_SHORT).show()
    }

    private fun saveFuel() {
        val km = kmField.text.toString().trim().replace(',', '.').toDoubleOrNull()
        val price = priceField.text.toString().trim().replace(',', '.').toDoubleOrNull()
        val amount = amountField.text.toString().trim().replace(',', '.').toDoubleOrNull()
        if (km == null || price == null || amount == null || km < 0 || price < 0 || amount <= 0) {
            Toast.makeText(this, "Inserisci KM, prezzo e importo validi", Toast.LENGTH_LONG).show(); return
        }
        val all = readRecords()
        all.put(JSONObject().apply {
            put("date", dateField.text.toString()); put("timestamp", System.currentTimeMillis())
            put("km", km); put("fuel", fuelField.selectedItem.toString())
            put("price", price); put("amount", amount); put("notes", notesField.text.toString())
        })
        prefs.edit().putString("records", all.toString()).apply()
        exportTxt(all)
        Toast.makeText(this, "Rifornimento salvato", Toast.LENGTH_SHORT).show()
        showArchive()
    }

    private fun showArchive() {
        val body = base("Auto Ago · Archivio")
        val records = (0 until readRecords().length()).map { readRecords().getJSONObject(it) }.sortedByDescending { it.optLong("timestamp") }
        if (records.isEmpty()) { body.addView(label("Nessun rifornimento registrato.")); return }
        val table = TableLayout(this).apply { isStretchAllColumns = false }
        val headings = arrayOf("Data", "KM", "Carb.", "€/L", "€", "KM/€", "Note")
        table.addView(row(headings, true))
        var previousKm: Double? = null
        records.forEach { r ->
            val km = r.optDouble("km")
            val kmEuro = previousKm?.let { (km - it) / r.optDouble("amount") }
            table.addView(row(arrayOf(r.optString("date"), format(km), shortFuel(r.optString("fuel")), format(r.optDouble("price"), 3), format(r.optDouble("amount"), 2), kmEuro?.let { format(it, 2) } ?: "—", r.optString("notes")), false))
            previousKm = km
        }
        body.addView(HorizontalScrollView(this).apply { addView(table) })
        val export = button("Salva TXT / aggiorna archivio") { exportTxt(readRecords()); Toast.makeText(this, "File aggiornato: ${txtFile().name}", Toast.LENGTH_SHORT).show() }
        body.addView(export, LinearLayout.LayoutParams(-1, 52).apply { setMargins(0, 16, 0, 8) })
    }

    private fun row(values: Array<String>, header: Boolean): TableRow = TableRow(this).apply {
        values.forEach { value -> addView(TextView(this@MainActivity).apply { text = value; textSize = if (header) 13f else 12f; setTextColor(if (header) 0xFFFFFFFF.toInt() else 0xFF263238.toInt()); setPadding(10, 12, 10, 12); setBackgroundColor(if (header) 0xFF1565C0.toInt() else 0xFFFFFFFF.toInt()) }) }
    }

    private fun readRecords() = try { JSONArray(prefs.getString("records", "[]")) } catch (_: Exception) { JSONArray() }
    private fun txtFile() = File(filesDir, "rifornimenti.txt")
    private fun exportTxt(records: JSONArray) { val sorted = (0 until records.length()).map { records.getJSONObject(it) }.sortedByDescending { it.optLong("timestamp") }; txtFile().writeText(buildString { appendLine("Data\tKM\tCarburante\tPrezzo/L\tImporto\tKM per €\tNote"); var prev: Double? = null; sorted.forEach { r -> val km = r.optDouble("km"); val amount = r.optDouble("amount"); appendLine("${r.optString("date")}\t${format(km)}\t${r.optString("fuel")}\t${format(r.optDouble("price"), 3)}\t${format(amount, 2)}\t${prev?.let { format((km-it)/amount, 2) } ?: "—"}\t${r.optString("notes").replace('\t', ' ')}"); prev = km } }) }
    private fun format(n: Double, decimals: Int = 0) = String.format(Locale.ITALY, "%.${decimals}f", n)
    private fun shortFuel(s: String) = when { s.startsWith("Metano") -> "met"; s.startsWith("Benzina") -> "benz"; else -> "Gas" }
    private fun label(text: String) = TextView(this).apply { this.text = text; textSize = 14f; setPadding(4, 12, 4, 4); setTextColor(0xFF455A64.toInt()) }
    private fun edit(hint: String, decimal: Boolean, content: String) = EditText(this).apply { this.hint = hint; this.textSize = 17f; this.setText(""); this.contentDescription = content; inputType = if (decimal) 8194 else 1; setPadding(14, 10, 14, 10) }
    private fun button(text: String, action: () -> Unit) = Button(this).apply { this.text = text; textSize = 12f; setOnClickListener { action() } }
}
