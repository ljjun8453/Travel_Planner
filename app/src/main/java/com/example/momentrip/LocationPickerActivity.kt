package com.example.momentrip

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.momentrip.databinding.ActivityLocationPickerBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityLocationPickerBinding
    private var googleMap: GoogleMap? = null
    private var selectedPlace = ""
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.action_select_location)

        binding.buttonSearchLocation.setOnClickListener { searchLocation() }
        binding.buttonCancelLocation.setOnClickListener { finish() }
        binding.buttonConfirmLocation.setOnClickListener { confirmLocation() }

        try {
            val mapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.locationMapContainer, mapFragment)
                .commitNow()
            mapFragment.getMapAsync(this)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_map_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val seoul = LatLng(37.5665, 126.9780)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, DEFAULT_ZOOM))
        map.setOnMapClickListener { position ->
            selectFromMap(position)
        }
    }

    private fun searchLocation() {
        val keyword = binding.editLocationSearch.text.toString().trim()
        if (keyword.isEmpty()) {
            Toast.makeText(this, R.string.toast_location_search_failed, Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressLocationPicker.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                findLocation(keyword)
            }

            binding.progressLocationPicker.visibility = View.GONE
            if (result == null) {
                Toast.makeText(this@LocationPickerActivity, R.string.toast_location_search_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            selectLocation(keyword, result.latitude, result.longitude)
        }
    }

    private fun selectFromMap(position: LatLng) {
        binding.progressLocationPicker.visibility = View.VISIBLE
        lifecycleScope.launch {
            val place = withContext(Dispatchers.IO) {
                findPlaceName(position.latitude, position.longitude)
            }

            binding.progressLocationPicker.visibility = View.GONE
            selectLocation(place, position.latitude, position.longitude)
        }
    }

    private fun selectLocation(place: String, latitude: Double, longitude: Double) {
        selectedPlace = place.ifBlank { getString(R.string.action_select_location) }
        selectedLatitude = latitude
        selectedLongitude = longitude
        binding.textPickerSelectedLocation.text = getString(R.string.selected_location_format, selectedPlace, latitude, longitude)
        googleMap?.clear()
        val position = LatLng(latitude, longitude)
        googleMap?.addMarker(MarkerOptions().position(position).title(selectedPlace))
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, SELECTED_ZOOM))
    }

    private fun confirmLocation() {
        val latitude = selectedLatitude
        val longitude = selectedLongitude
        if (latitude == null || longitude == null) {
            Toast.makeText(this, R.string.toast_location_required, Toast.LENGTH_SHORT).show()
            return
        }

        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(EXTRA_PLACE, selectedPlace)
                .putExtra(EXTRA_LATITUDE, latitude)
                .putExtra(EXTRA_LONGITUDE, longitude)
        )
        finish()
    }

    @Suppress("DEPRECATION")
    private fun findLocation(keyword: String): LatLng? {
        return try {
            val address = Geocoder(this, Locale.KOREA).getFromLocationName(keyword, 1)?.firstOrNull()
            if (address == null) null else LatLng(address.latitude, address.longitude)
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun findPlaceName(latitude: Double, longitude: Double): String {
        return try {
            val address = Geocoder(this, Locale.KOREA).getFromLocation(latitude, longitude, 1)?.firstOrNull()
            address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: address?.featureName ?: "$latitude, $longitude"
        } catch (_: Exception) {
            "$latitude, $longitude"
        }
    }

    companion object {
        const val EXTRA_PLACE = "place"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        private const val DEFAULT_ZOOM = 6f
        private const val SELECTED_ZOOM = 13f

        fun createIntent(context: Context): Intent {
            return Intent(context, LocationPickerActivity::class.java)
        }
    }
}
