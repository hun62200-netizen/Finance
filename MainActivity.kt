package com.neon.finance

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val items = mutableListOf<JSONObject>()
    private val rows = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private lateinit var balanceView: TextView
    private lateinit var summaryView: TextView
    private lateinit var descInput: EditText
    private lateinit var amountInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        balanceView = findViewById(R.id.balance)
        summaryView = findViewById(R.id.summary)
        descInput = findViewById(R.id.desc)
        amountInput = findViewById(R.id.amount)
        val list = findViewById<ListView>(R.id.list)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
        list.adapter = adapter

        findViewById<Button>(R.id.btnIncome).setOnClickListener { add(true) }
        findViewById<Button>(R.id.btnExpense).setOnClickListener { add(false) }

        list.setOnItemLongClickListener { _, _, position, _ ->
            items.removeAt(position)
            save()
            refresh()
            true
        }

        load()
        refresh()
    }

    private fun add(income: Boolean) {
        val amount = amountInput.text.toString().replace(',', '.').toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        val desc = descInput.text.toString().trim().ifEmpty { if (income) "Income" else "Expense" }
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        items.add(0, JSONObject().apply {
            put("income", income)
            put("amount", amount)
            put("desc", desc)
            put("date", date)
        })
        save()
        refresh()
        descInput.text.clear()
        amountInput.text.clear()
    }

    private fun refresh() {
        var inc = 0.0
        var exp = 0.0
        rows.clear()
        for (t in items) {
            val a = t.getDouble("amount")
            val isIncome = t.getBoolean("income")
            if (isIncome) inc += a else exp += a
            val sign = if (isIncome) "+" else "-"
            rows.add("${t.getString("date")}   $sign${fmt(a)}   ${t.getString("desc")}")
        }
        adapter.notifyDataSetChanged()
        balanceView.text = fmt(inc - exp)
        summaryView.text = "Income: ${fmt(inc)}    Expenses: ${fmt(exp)}"
    }

    private fun fmt(v: Double) = String.format(Locale.US, "%,.2f", v)

    private fun save() {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        getSharedPreferences("finance", MODE_PRIVATE).edit().putString("items", arr.toString()).apply()
    }

    private fun load() {
        val raw = getSharedPreferences("finance", MODE_PRIVATE).getString("items", null) ?: return
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
    }
}
