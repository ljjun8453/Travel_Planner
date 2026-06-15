package com.example.momentrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.momentrip.databinding.FragmentTravelMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlanMapFragment : Fragment() {
    private var _binding: FragmentTravelMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: TravelDBHelper
    private var googleMap: GoogleMap? = null
    private var plans = listOf<TravelPlan>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTravelMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dbHelper = TravelDBHelper(requireContext())

        binding.textTravelMapBadge.text = "위치"
        binding.textTravelMapTitle.text = "여행 계획 지도"
        binding.textTravelMapDescription.text = "위치가 저장된 여행 계획을 지도에서 확인하세요."

        setupMap()
    }

    override fun onResume() {
        super.onResume()
        loadPlans()
    }

    override fun onDestroyView() {
        dbHelper.close()
        googleMap = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupMap() {
        try {
            val existMapFragment = childFragmentManager.findFragmentByTag(MAP_TAG) as? SupportMapFragment

            val mapFragment = existMapFragment ?: SupportMapFragment.newInstance().also {
                childFragmentManager.beginTransaction()
                    .replace(R.id.googleMapContainer, it, MAP_TAG)
                    .commitNow()
            }

            mapFragment.getMapAsync { map ->
                googleMap = map
                setupMapOptions(map)
                renderMarkers()
            }
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "지도를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMapOptions(map: GoogleMap) {
        try {
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = false
            map.uiSettings.isCompassEnabled = true

            map.setPadding(0, 0, 0, dp(120))
        } catch (_: Exception) {
        }
    }

    private fun loadPlans() {
        val currentBinding = _binding ?: return
        currentBinding.progressTravelMap.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Pair(dbHelper.getAllPlans(), false)
                } catch (_: Exception) {
                    Pair(emptyList<TravelPlan>(), true)
                }
            }

            val latestBinding = _binding ?: return@launch

            if (!isAdded) {
                return@launch
            }

            latestBinding.progressTravelMap.visibility = View.GONE

            if (result.second) {
                Toast.makeText(requireContext(), R.string.toast_load_failed, Toast.LENGTH_SHORT).show()
            }

            plans = result.first.filter { plan ->
                plan.latitude != null && plan.longitude != null
            }

            renderMarkers()
        }
    }

    private fun renderMarkers() {
        val map = googleMap ?: return
        val currentBinding = _binding ?: return

        try {
            map.clear()
            setupMapOptions(map)

            if (plans.isEmpty()) {
                currentBinding.textTravelMapMessage.text = "저장된 위치가 없습니다."
                currentBinding.textTravelMapGuide.text = "위치를 선택한 여행 계획이 지도에 표시됩니다."
                currentBinding.mapInfoPanel.visibility = View.VISIBLE
                return
            }

            currentBinding.textTravelMapMessage.text = "${plans.size}개 장소가 저장되어 있습니다."
            currentBinding.textTravelMapGuide.text = makeLocationGuide(plans)
            currentBinding.mapInfoPanel.visibility = View.VISIBLE

            val boundsBuilder = LatLngBounds.Builder()
            var markerCount = 0
            var firstLatLng: LatLng? = null

            plans.forEach { plan ->
                val latitude = plan.latitude ?: return@forEach
                val longitude = plan.longitude ?: return@forEach

                val latLng = LatLng(latitude, longitude)

                if (firstLatLng == null) {
                    firstLatLng = latLng
                }

                boundsBuilder.include(latLng)
                markerCount++

                map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(plan.place)
                        .snippet(plan.planDate)
                )
            }

            map.setOnMarkerClickListener { marker ->
                marker.showInfoWindow()
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 13f))
                true
            }

            if (markerCount == 1 && firstLatLng != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLatLng!!, 13f))
            } else if (markerCount > 1) {
                val bounds = boundsBuilder.build()
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, dp(80)))
            }
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "지도 표시 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeLocationGuide(plans: List<TravelPlan>): String {
        return try {
            plans.take(3).joinToString("\n") { plan ->
                val latitude = plan.latitude ?: 0.0
                val longitude = plan.longitude ?: 0.0
                "${plan.place} · $latitude, $longitude"
            }
        } catch (_: Exception) {
            "위치가 저장된 계획을 지도에서 확인하세요."
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val MAP_TAG = "travel_plan_map"
    }
}