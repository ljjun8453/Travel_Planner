package com.example.momentrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TravelMapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var dbHelper: TravelDBHelper
    private lateinit var progressBar: ProgressBar
    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_travel_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dbHelper = TravelDBHelper(requireContext())
        progressBar = view.findViewById(R.id.progressTravelMap)
        view.findViewById<TextView>(R.id.textTravelMapTitle).setText(R.string.map_title)
        view.findViewById<TextView>(R.id.textTravelMapDescription).setText(R.string.map_description)

        try {
            val mapFragment = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.googleMapContainer, mapFragment)
                .commitNow()
            mapFragment.getMapAsync(this)
        } catch (_: Exception) {
            view.findViewById<TextView>(R.id.textTravelMapMessage).setText(R.string.toast_map_failed)
            Toast.makeText(requireContext(), R.string.toast_map_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        try {
            googleMap = map
            loadLocations()
        } catch (_: Exception) {
            view?.findViewById<TextView>(R.id.textTravelMapMessage)?.setText(R.string.toast_map_failed)
            context?.let { Toast.makeText(it, R.string.toast_map_failed, Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onResume() {
        super.onResume()
        loadLocations()
    }

    override fun onDestroyView() {
        googleMap = null
        dbHelper.close()
        super.onDestroyView()
    }

    private fun loadLocations() {
        if (view == null || !::dbHelper.isInitialized || !::progressBar.isInitialized) {
            return
        }
        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Pair(dbHelper.getTravelsWithLocation(), false)
                } catch (_: Exception) {
                    Pair(emptyList<TravelRecord>(), true)
                }
            }

            if (!isAdded || view == null) {
                return@launch
            }
            progressBar.visibility = View.GONE
            if (result.second) {
                Toast.makeText(requireContext(), R.string.toast_map_failed, Toast.LENGTH_SHORT).show()
            }
            renderLocations(result.first)
        }
    }

    private fun renderLocations(records: List<TravelRecord>) {
        try {
            val message = view?.findViewById<TextView>(R.id.textTravelMapMessage) ?: return
            val guide = view?.findViewById<TextView>(R.id.textTravelMapGuide) ?: return
            val map = googleMap

            if (records.isEmpty()) {
                message.setText(R.string.map_empty_title)
                guide.setText(R.string.map_empty_message)
                map?.clear()
                return
            }

            val latest = records.last()
            message.text = getString(R.string.map_count_message, records.size)
            guide.text = getString(R.string.map_latest_message, latest.place, latest.latitude, latest.longitude)

            if (map == null) {
                return
            }

            map.clear()
            records.forEach { record ->
                val latitude = record.latitude
                val longitude = record.longitude
                if (latitude != null && longitude != null) {
                    val position = LatLng(latitude, longitude)
                    map.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(record.place)
                            .snippet(record.visitDate)
                    )
                }
            }
            val latestPosition = LatLng(latest.latitude ?: 0.0, latest.longitude ?: 0.0)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latestPosition, DEFAULT_ZOOM))
        } catch (_: Exception) {
            progressBar.visibility = View.GONE
            view?.findViewById<TextView>(R.id.textTravelMapMessage)?.setText(R.string.toast_map_failed)
            context?.let { Toast.makeText(it, R.string.toast_map_failed, Toast.LENGTH_SHORT).show() }
        }
    }

    companion object {
        private const val DEFAULT_ZOOM = 10f
    }
}
