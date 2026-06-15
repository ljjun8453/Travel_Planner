package com.example.momentrip

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.momentrip.databinding.ActivityDetailBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var dbHelper: TravelDBHelper

    private var travelNo: Int = -1
    private var currentRecord: TravelRecord? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = "여행 기록 상세"
        dbHelper = TravelDBHelper(this)

        travelNo = intent.getIntExtra(EXTRA_TRAVEL_NO, -1)

        binding.buttonEditRecord.setOnClickListener {
            val record = currentRecord ?: return@setOnClickListener

            try {
                AddEditActivity.start(this, record.no)
            } catch (_: Exception) {
                Toast.makeText(this, R.string.error_screen_change, Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonDeleteRecord.setOnClickListener {
            val record = currentRecord ?: return@setOnClickListener
            confirmDelete(record)
        }

        loadRecord()
    }

    override fun onResume() {
        super.onResume()

        if (travelNo > 0) {
            loadRecord()
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun loadRecord() {
        if (travelNo <= 0) {
            Toast.makeText(this, R.string.toast_load_failed, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressDetail.visibility = View.VISIBLE

        lifecycleScope.launch {
            val record = withContext(Dispatchers.IO) {
                try {
                    dbHelper.getTravel(travelNo)
                } catch (_: Exception) {
                    null
                }
            }

            binding.progressDetail.visibility = View.GONE

            if (record == null) {
                Toast.makeText(this@DetailActivity, R.string.toast_load_failed, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            currentRecord = record
            renderRecord(record)
        }
    }

    private fun renderRecord(record: TravelRecord) {
        binding.detailTextPlace.text = record.place
        binding.detailTextVisitDate.text = record.visitDate

        binding.detailTextMemo.text = if (record.memo.isNullOrBlank()) {
            getString(R.string.item_no_memo)
        } else {
            record.memo
        }

        setDetailImage(record)

        if (record.latitude != null && record.longitude != null) {
            binding.detailTextLocation.text = "위도 ${record.latitude} · 경도 ${record.longitude}"
            binding.detailMapContainer.visibility = View.VISIBLE
            binding.detailMapNotice.visibility = View.GONE
            setupMap(record)
        } else {
            binding.detailTextLocation.text = getString(R.string.item_no_location)
            binding.detailMapContainer.visibility = View.GONE
            binding.detailMapNotice.visibility = View.VISIBLE
        }
    }

    private fun setDetailImage(record: TravelRecord) {
        try {
            if (record.photoUri.isNullOrBlank()) {
                binding.detailImagePhoto.setImageResource(R.drawable.momentrip1)
            } else {
                binding.detailImagePhoto.setImageURI(Uri.parse(record.photoUri))
            }
        } catch (_: Exception) {
            binding.detailImagePhoto.setImageResource(R.drawable.momentrip1)
        }
    }

    private fun setupMap(record: TravelRecord) {
        try {
            val latitude = record.latitude ?: return
            val longitude = record.longitude ?: return
            val latLng = LatLng(latitude, longitude)

            val mapFragment = SupportMapFragment.newInstance()

            supportFragmentManager.beginTransaction()
                .replace(R.id.detailMapContainer, mapFragment)
                .commitAllowingStateLoss()

            mapFragment.getMapAsync { googleMap ->
                try {
                    googleMap.uiSettings.isMapToolbarEnabled = false
                    googleMap.uiSettings.isZoomControlsEnabled = false
                    googleMap.uiSettings.isMyLocationButtonEnabled = false

                    googleMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(record.place)
                            .snippet(record.visitDate)
                    )

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                } catch (_: Exception) {
                    Toast.makeText(this, "지도를 표시하지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {
            Toast.makeText(this, "지도를 표시하지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(record: TravelRecord) {
        try {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, record.place))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    deleteRecord(record)
                }
                .show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteRecord(record: TravelRecord) {
        binding.progressDetail.visibility = View.VISIBLE

        lifecycleScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                try {
                    dbHelper.deleteTravel(record.no) > 0
                } catch (_: Exception) {
                    false
                }
            }

            binding.progressDetail.visibility = View.GONE

            if (deleted) {
                Toast.makeText(this@DetailActivity, R.string.toast_deleted, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@DetailActivity, R.string.toast_delete_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val EXTRA_TRAVEL_NO = "extra_travel_no"

        fun start(context: Context, no: Int) {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra(EXTRA_TRAVEL_NO, no)
            context.startActivity(intent)
        }
    }
}