package com.example.momentrip

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.momentrip.databinding.ActivityDetailBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var dbHelper: TravelDBHelper
    private var recordNo = 0
    private var currentRecord: TravelRecord? = null
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.detail_title)

        dbHelper = TravelDBHelper(this)
        recordNo = intent.getIntExtra(EXTRA_RECORD_NO, 0)
        setupDetailMap()

        binding.buttonEditRecord.setOnClickListener {
            try {
                currentRecord?.let { record -> AddEditActivity.start(this, record.no) }
            } catch (_: Exception) {
                Toast.makeText(this, R.string.error_screen_change, Toast.LENGTH_SHORT).show()
            }
        }
        binding.buttonDeleteRecord.setOnClickListener {
            currentRecord?.let { record -> confirmDelete(record) }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        try {
            googleMap = map
            currentRecord?.let { showDetailMap(it) }
        } catch (_: Exception) {
            binding.detailMapNotice.visibility = View.VISIBLE
            binding.detailMapNotice.setText(R.string.toast_map_failed)
            Toast.makeText(this, R.string.toast_map_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecord()
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

    private fun setupDetailMap() {
        try {
            val mapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.detailMapContainer, mapFragment)
                .commitNow()
            mapFragment.getMapAsync(this)
        } catch (_: Exception) {
            binding.detailMapContainer.visibility = View.GONE
            binding.detailMapNotice.visibility = View.VISIBLE
            binding.detailMapNotice.setText(R.string.toast_map_failed)
            Toast.makeText(this, R.string.toast_map_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadRecord() {
        binding.progressDetail.visibility = View.VISIBLE
        lifecycleScope.launch {
            val record = withContext(Dispatchers.IO) {
                try {
                    dbHelper.getTravel(recordNo)
                } catch (_: Exception) {
                    null
                }
            }

            binding.progressDetail.visibility = View.GONE
            if (record == null) {
                Toast.makeText(this@DetailActivity, R.string.toast_record_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            showRecord(record)
        }
    }

    private fun showRecord(record: TravelRecord) {
        currentRecord = record
        binding.detailTextPlace.text = record.place
        binding.detailTextVisitDate.text = record.visitDate
        binding.detailTextMemo.text = record.memo ?: getString(R.string.item_no_memo)
        binding.detailTextLocation.text = if (record.latitude != null && record.longitude != null) {
            getString(R.string.detail_location_format, record.latitude, record.longitude)
        } else {
            getString(R.string.detail_no_location)
        }
        showPhoto(record.photoUri)
        showDetailMap(record)
    }

    private fun showPhoto(photoUri: String?) {
        try {
            val firstPhoto = TravelPhotoStore.first(photoUri)
            if (firstPhoto.isNullOrBlank()) {
                binding.detailImagePhoto.setImageResource(R.drawable.ic_launcher_foreground)
            } else {
                binding.detailImagePhoto.setImageURI(Uri.parse(firstPhoto))
            }
        } catch (_: Exception) {
            binding.detailImagePhoto.setImageResource(R.drawable.ic_launcher_foreground)
            Toast.makeText(this, R.string.toast_image_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDetailMap(record: TravelRecord) {
        try {
            val latitude = record.latitude
            val longitude = record.longitude
            if (latitude == null || longitude == null) {
                googleMap?.clear()
                binding.detailMapContainer.visibility = View.GONE
                binding.detailMapNotice.visibility = View.VISIBLE
                binding.detailMapNotice.setText(R.string.detail_no_location)
                return
            }

            binding.detailMapContainer.visibility = View.VISIBLE
            binding.detailMapNotice.visibility = View.GONE
            val map = googleMap ?: return
            val position = LatLng(latitude, longitude)
            map.clear()
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(record.place)
                    .snippet(record.visitDate)
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, DETAIL_ZOOM))
        } catch (_: Exception) {
            binding.detailMapContainer.visibility = View.GONE
            binding.detailMapNotice.visibility = View.VISIBLE
            binding.detailMapNotice.setText(R.string.toast_map_failed)
            Toast.makeText(this, R.string.toast_map_failed, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, R.string.toast_delete_failed, Toast.LENGTH_SHORT).show()
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
        private const val EXTRA_RECORD_NO = "record_no"
        private const val DETAIL_ZOOM = 14f

        fun start(context: Context, recordNo: Int) {
            context.startActivity(Intent(context, DetailActivity::class.java).putExtra(EXTRA_RECORD_NO, recordNo))
        }
    }
}
