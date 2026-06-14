package com.example.momentrip

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEditPlanActivity : AppCompatActivity() {
    private lateinit var dbHelper: TravelDBHelper
    private lateinit var editPlace: EditText
    private lateinit var editDate: EditText
    private lateinit var editMemo: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.plan_edit_title_new)

        dbHelper = TravelDBHelper(this)
        editPlace = findViewById(R.id.editPlanPlace)
        editDate = findViewById(R.id.editPlanDate)
        editMemo = findViewById(R.id.editPlanMemo)
        progressBar = findViewById(R.id.progressPlanEdit)
        editDate.setOnClickListener { showDatePicker() }
        findViewById<Button>(R.id.buttonCancelPlan).setOnClickListener { finish() }
        findViewById<Button>(R.id.buttonSavePlan).setOnClickListener { savePlan() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun showDatePicker() {
        try {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    editDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePlan() {
        val place = editPlace.text.toString().trim()
        val planDate = editDate.text.toString().trim()
        if (place.isEmpty() || planDate.isEmpty()) {
            Toast.makeText(this, R.string.toast_plan_place_date_required, Toast.LENGTH_SHORT).show()
            return
        }

        val plan = TravelPlan(
            place = place,
            planDate = planDate,
            memo = editMemo.text.toString().trim().ifEmpty { null }
        )

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    dbHelper.insertPlan(plan)
                } catch (_: Exception) {
                    -1L
                }
            }

            progressBar.visibility = View.GONE
            if (result > 0) {
                Toast.makeText(this@AddEditPlanActivity, R.string.toast_plan_saved, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@AddEditPlanActivity, R.string.toast_plan_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AddEditPlanActivity::class.java))
        }
    }
}
