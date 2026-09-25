package it.autoago

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("auto_ago", MODE_PRIVATE) }
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)

    private lateinit var dateField: TextView
    private lateinit var kmField: EditText
    private lateinit var fuelField: Spinner
    private lateinit var priceField: EditText
    private lateinit var amountField: EditText
    private lateinit var notesField: EditText

    private val blue = Color.rgb(91, 112, 255)
    private val purple = Color.rgb(123, 90, 197)
    private val red = Color.rgb(214, 45, 52)
    private val dark = Color.rgb(26, 31, 46)
    private val slate = Color.rgb(96, 108, 128)
    private val page = Color.rgb(245, 247, 250)
    private val white = Color.rgb(255, 255, 255)
    private val stroke = Color.rgb(226, 232, 240)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showEntry()
    }

    private fun showEntry() {
        val root = buildRoot("Nuovo rifornimento", "Dati giornalieri del tuo veicolo")
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            background = roundedCard(white, 28f)
            elevation = 8f
        }

        card.addView(labelSmall("DATA E ORA"))
        dateField = TextView(this).apply {
            text = dateFormat.format(java.util.Date())
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(dark)
            textSize = 17f
            setPadding(18, 16, 18, 16)
            background = roundedField(white, 16f, stroke, 1)
            typeface = Typeface.DEFAULT_BOLD
        }
        card.addView(dateField, fieldLayout(62))

        card.addView(labelSmall("KM E CARBURANTE"))
        kmField = inputField("KM letti dal cruscotto", false)
        card.addView(kmField, fieldLayout(60))

        fuelField = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, arrayOf("Metano (met)", "Benzina (benz)", "Gasolio (Gas)"))
            background = roundedField(white, 16f, stroke, 1)
        }
        card.addView(fuelField, fieldLayout(60))

        card.addView(labelSmall("PREZZO E IMPORTO"))
        priceField = inputField("Prezzo al litro  €", true)
        card.addView(priceField, fieldLayout(60))

        amountField = inputField("Importo pagato  €", true)
        card.addView(amountField, fieldLayout(60))

        card.addView(labelSmall("NOTE / MANUTENZIONI"))
        notesField = inputField("Dettagli o manutenzioni", false).apply {
            minLines = 3
            setSingleLine(false)
            gravity = Gravity.TOP or Gravity.START
            setPadding(18, 16, 18, 16)
        }
        card.addView(notesField, fieldLayout(120))

        val saveButton = Button(this).apply {
            text = "SALVA RIFORNIMENTO"
            typeface = Typeface.DEFAULT_BOLD
            textSize = 16f
            setTextColor(white)
            isAllCaps = false
            background = gradientButton(red, purple)
            setOnClickListener { saveFuel() }
            compoundDrawablePadding = 10
            setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0)
        }
        root.addView(card)
        root.addView(saveButton, ViewGroup.LayoutParams.MATCH_PARENT, 60)
        saveButton.layoutParams = (saveButton.layoutParams as? ViewGroup.MarginLayoutParams)?.apply { setMargins(0, 18, 0, 0) } ?: ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 60).apply { setMargins(0, 18, 0, 0) }

        setContentView(root)
    }

    private fun showArchive() {
        val root = buildRoot("Archivio rifornimenti", "Ordine cronologico: più recente in alto")
        val records = recordsSorted()

        if (records.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Nessun rifornimento registrato" 
                setTextColor(slate)
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 40)
            }
            root.addView(empty)
        } else {
            val tableCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedCard(white, 24f)
                setPadding(10, 10, 10, 10)
                elevation = 6f
            }

            val wrapper = HorizontalScrollView(this)
            val table = TableLayout(this).apply { isStretchAllColumns = false }
            table.addView(rowHeader(arrayOf("DATA", "KM", "CARB", "€/L", "€", "KM/€", "NOTE")))

            val kmPerEuroById = kmPerEuroMap(records)
            records.forEach { record ->
                val kmValue = record.optDouble("km")
                val note = record.optString("notes").ifBlank { "-" }
                val fuel = shortFuel(record.optString("fuel"))
                val kmEuro = kmPerEuroById[record.optLong("timestamp")] ?: 0.0
                table.addView(
                    rowData(
                        arrayOf(
                            record.optString("date"),
                            fmt(kmValue),
                            fuel,
                            fmt(record.optDouble("price"), 3),
                            fmt(record.optDouble("amount"), 2),
                            fmt(kmEuro, 2),
                            note
                        )
                    )
                )
            }

            wrapper.addView(table)
            tableCard.addView(wrapper)
            root.addView(tableCard)
        }

        val exportButton = Button(this).apply {
            text = "ESPORTA TXT"
            typeface = Typeface.DEFAULT_BOLD
            textSize = 15f
            setTextColor(blue)
            background = roundedField(white, 16f, stroke, 1)
            setOnClickListener { chooseExportLocation() }
            isAllCaps = false
        }
        root.addView(exportButton, ViewGroup.LayoutParams.MATCH_PARENT, 54)
        exportButton.layoutParams = (exportButton.layoutParams as? ViewGroup.MarginLayoutParams)?.apply { setMargins(0, 18, 0, 0) } ?: ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 54).apply { setMargins(0, 18, 0, 0) }

        val note = TextView(this).apply {
            text = "Il file TXT viene creato e aggiornato automaticamente; puoi salvarlo in Download o in una cartella a tua scelta."
            textSize = 12f
            setTextColor(slate)
            setPadding(6, 14, 6, 0)
        }
        root.addView(note)
        setContentView(root)
    }

    private fun buildRoot(title: String, subtitle: String): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(page)
            setPadding(18, 20, 18, 22)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 8)
        }

        val appIcon = ImageView(this).apply {
            setImageResource(R.drawable.app_icon)
            layoutParams = LinearLayout.LayoutParams(58, 58)
            adjustViewBounds = true
        }
        topBar.addView(appIcon)

        val titleWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 0, 0, 0)
        }
        titleWrap.addView(TextView(this).apply {
            text = "AUTO AGO"
            setTextColor(dark)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        })
        titleWrap.addView(TextView(this).apply {
            text = title
            setTextColor(red)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        })
        topBar.addView(titleWrap, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        nav.addView(navButton("Nuovo", true) { showEntry() }, LinearLayout.LayoutParams(0, 46, 1f).apply { setMargins(0,0,8,0) })
        nav.addView(navButton("Archivio", false) { showArchive() }, LinearLayout.LayoutParams(0, 46, 1f))

        root.addView(topBar)
        root.addView(TextView(this).apply {
            text = subtitle
            setTextColor(slate)
            textSize = 13f
            setPadding(6, 0, 0, 0)
        })
        root.addView(nav)

        return root
    }

    private fun navButton(title: String, active: Boolean, action: () -> Unit): Button = Button(this).apply {
        text = title
        isAllCaps = false
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (active) white else dark)
        background = if (active) gradientButton(red, purple) else roundedField(white, 16f, stroke, 1)
        setOnClickListener { action() }
    }

    private fun saveFuel() {
        val kmValue = parseDecimal(kmField)
        val priceValue = parseDecimal(priceField)
        val amountValue = parseDecimal(amountField)

        if (kmValue == null || priceValue == null || amountValue == null) {
            Toast.makeText(this, "Inserisci KM, prezzo e importo validi", Toast.LENGTH_LONG).show()
            return
        }

        if (kmValue < 0 || priceValue < 0 || amountValue <= 0) {
            Toast.makeText(this, "Controlla i valori inseriti", Toast.LENGTH_LONG).show()
            return
        }

        val records = readRecords()
        val data = JSONObject().apply {
            put("date", dateField.text.toString())
            put("timestamp", System.currentTimeMillis())
            put("km", kmValue)
            put("fuel", fuelField.selectedItem.toString())
            put("price", priceValue)
            put("amount", amountValue)
            put("notes", notesField.text.toString())
        }
        records.put(data)
        prefs.edit().putString("records", records.toString()).apply()
        writeInternalTxt(records)
        Toast.makeText(this, "Rifornimento salvato", Toast.LENGTH_SHORT).show()
        showArchive()
    }

    private fun chooseExportLocation() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "rifornimenti.txt")
        }
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val content = exportText(readRecords())
            try {
                contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                Toast.makeText(this, "File TXT salvato correttamente", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(this, "Impossibile salvare il file TXT", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun readRecords(): JSONArray = try {
        JSONArray(prefs.getString("records", "[]"))
    } catch (_: Exception) {
        JSONArray()
    }

    private fun recordsSorted(): List<JSONObject> = readRecords().let { array ->
        (0 until array.length()).map { array.getJSONObject(it) }
            .sortedByDescending { it.optLong("timestamp") }
    }

    private fun kmPerEuroMap(records: List<JSONObject>): Map<Long, Double> {
        val ordered = records.sortedBy { it.optLong("timestamp") }
        val map = linkedMapOf<Long, Double>()
        var previousKm: Double? = null

        ordered.forEach { item ->
            val currentKm = item.optDouble("km")
            val currentAmount = item.optDouble("amount")
            val value = if (previousKm != null && currentAmount > 0.0) {
                (currentKm - previousKm!!) / currentAmount
            } else {
                0.0
            }
            map[item.optLong("timestamp")] = value
            previousKm = currentKm
        }
        return map
    }

    private fun writeInternalTxt(records: JSONArray) {
        val file = File(filesDir, "rifornimenti.txt")
        file.writeText(exportText(records), Charsets.UTF_8)
    }

    private fun exportText(records: JSONArray): String {
        val sorted = (0 until records.length()).map { records.getJSONObject(it) }.sortedByDescending { it.optLong("timestamp") }
        val lines = arrayListOf<String>()
        lines.add("Data\tKM\tCarburante\tPrezzo/L\tImporto\tKM per €\tNote")

        var previousKm: Double? = null
        sorted.forEach { item ->
            val km = item.optDouble("km")
            val amount = item.optDouble("amount")
            val kmPerEuro = if (previousKm != null && amount > 0.0) (km - previousKm!!) / amount else 0.0
            val note = item.optString("notes").replace("\t", " ")
            lines.add("${item.optString("date")}\t${fmt(km)}\t${item.optString("fuel")}\t${fmt(item.optDouble("price"), 3)}\t${fmt(amount, 2)}\t${fmt(kmPerEuro, 2)}\t${note}")
            previousKm = km
        }
        return lines.joinToString(System.lineSeparator()) + System.lineSeparator()
    }

    private fun inputField(hint: String, decimal: Boolean): EditText = EditText(this).apply {
        this.hint = hint
        setTextColor(dark)
        setHintTextColor(slate)
        textSize = 16f
        setPadding(18, 16, 18, 16)
        background = roundedField(white, 16f, stroke, 1)
        inputType = if (decimal) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_NUMBER
    }

    private fun parseDecimal(editText: EditText): Double? {
        return editText.text.toString().trim().replace(',', '.').toDoubleOrNull()
    }

    private fun rowHeader(values: Array<String>): TableRow = TableRow(this).apply {
        values.forEach { value ->
            addView(TextView(this@MainActivity).apply {
                text = value
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(white)
                textSize = 11f
                setPadding(10, 12, 10, 12)
                background = GradientDrawable().apply {
                    setColor(red)
                    shape = GradientDrawable.RECTANGLE
                }
            })
        }
    }

    private fun rowData(values: Array<String>): TableRow = TableRow(this).apply {
        values.forEach { value ->
            addView(TextView(this@MainActivity).apply {
                text = value
                setTextColor(dark)
                textSize = 12f
                setPadding(10, 12, 10, 12)
                background = roundedField(white, 0f, stroke, 1)
            })
        }
    }

    private fun labelSmall(value: String): TextView = TextView(this).apply {
        text = value
        setTextColor(red)
        textSize = 11f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(4, 18, 4, 8)
    }

    private fun fieldLayout(height: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        height
    ).apply { setMargins(0, 0, 0, 10) }

    private fun roundedField(color: Int, radius: Float, strokeColor: Int, strokeWidth: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        setStroke(strokeWidth, strokeColor)
    }

    private fun roundedCard(color: Int, radius: Float): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun gradientButton(start: Int, end: Int): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(start, end)
    ).apply {
        cornerRadius = 18f
    }

    private fun shortFuel(value: String): String = when {
        value.startsWith("Metano") -> "met"
        value.startsWith("Benzina") -> "benz"
        else -> "Gas"
    }

    private fun fmt(value: Double, decimals: Int = 0): String = String.format(Locale.ITALY, "%.${decimals}f", value)
}
