package com.example.momentrip

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.momentrip.databinding.ActivityPlanEditBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEditPlanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlanEditBinding
    private lateinit var dbHelper: TravelDBHelper
    private var planNo = 0
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedPlaceName = ""

    private val locationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val place = data.getStringExtra(LocationPickerActivity.EXTRA_PLACE).orEmpty()
                val latitude = data.getDoubleExtra(LocationPickerActivity.EXTRA_LATITUDE, Double.NaN)
                val longitude = data.getDoubleExtra(LocationPickerActivity.EXTRA_LONGITUDE, Double.NaN)
                if (!latitude.isNaN() && !longitude.isNaN()) {
                    selectedPlaceName = place
                    selectedLatitude = latitude
                    selectedLongitude = longitude
                    if (binding.editPlanPlace.text.toString().trim().isEmpty() && place.isNotBlank()) {
                        binding.editPlanPlace.setText(place)
                    }
                    updateSelectedLocationText()
                }
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_location_search_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbHelper = TravelDBHelper(this)
        planNo = intent.getIntExtra(EXTRA_PLAN_NO, 0)
        binding.editPlanDate.setOnClickListener { showDateTimePicker() }
        binding.buttonSelectPlanLocation.setOnClickListener { openLocationPicker() }
        binding.buttonCancelPlan.setOnClickListener { finish() }
        binding.buttonSavePlan.setOnClickListener { savePlan() }

        if (planNo > 0) {
            title = getString(R.string.plan_edit_title_update)
            binding.textPlanEditTitle.setText(R.string.plan_edit_title_update)
            loadPlan()
        } else {
            title = getString(R.string.plan_edit_title_new)
            binding.textPlanEditTitle.setText(R.string.plan_edit_title_new)
        }
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

    private fun showDateTimePicker() {
        try {
            DateTimeRangePicker.show(this) { value ->
                binding.editPlanDate.setText(value)
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openLocationPicker() {
        try {
            locationLauncher.launch(LocationPickerActivity.createIntent(this))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_screen_change, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPlan() {
        binding.progressPlanEdit.visibility = View.VISIBLE
        lifecycleScope.launch {
            val plan = withContext(Dispatchers.IO) {
                try {
                    dbHelper.getPlan(planNo)
                } catch (_: Exception) {
                    null
                }
            }

            binding.progressPlanEdit.visibility = View.GONE
            if (plan == null) {
                Toast.makeText(this@AddEditPlanActivity, R.string.toast_plan_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            binding.editPlanPlace.setText(plan.place)
            binding.editPlanDate.setText(plan.planDate)
            binding.editPlanMemo.setText(plan.memo.orEmpty())
            selectedLatitude = plan.latitude
            selectedLongitude = plan.longitude
            selectedPlaceName = plan.place
            updateSelectedLocationText()
        }
    }

    private fun savePlan() {
        val place = binding.editPlanPlace.text.toString().trim()
        val planDate = binding.editPlanDate.text.toString().trim()
        if (place.isEmpty() || planDate.isEmpty()) {
            Toast.makeText(this, R.string.toast_plan_place_date_required, Toast.LENGTH_SHORT).show()
            return
        }

        val plan = TravelPlan(
            no = planNo,
            place = place,
            planDate = planDate,
            memo = binding.editPlanMemo.text.toString().trim().ifEmpty { null },
            latitude = selectedLatitude,
            longitude = selectedLongitude
        )

        binding.progressPlanEdit.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    if (planNo > 0) dbHelper.updatePlan(plan).toLong() else dbHelper.insertPlan(plan)
                } catch (_: Exception) {
                    -1L
                }
            }

            binding.progressPlanEdit.visibility = View.GONE
            if (result > 0) {
                Toast.makeText(this@AddEditPlanActivity, R.string.toast_plan_saved, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@AddEditPlanActivity, R.string.toast_plan_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSelectedLocationText() {
        val latitude = selectedLatitude
        val longitude = selectedLongitude
        binding.textPlanSelectedLocation.text = if (latitude == null || longitude == null) {
            getString(R.string.selected_location_empty)
        } else {
            getString(R.string.selected_location_format, selectedPlaceName.ifBlank { binding.editPlanPlace.text.toString().trim() }, latitude, longitude)
        }
    }

    companion object {
        private const val EXTRA_PLAN_NO = "plan_no"

        fun start(context: Context, planNo: Int = 0) {
            context.startActivity(Intent(context, AddEditPlanActivity::class.java).putExtra(EXTRA_PLAN_NO, planNo))
        }
    }
}
