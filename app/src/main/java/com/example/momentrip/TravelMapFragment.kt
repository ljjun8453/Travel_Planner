package com.example.momentrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class TravelMapFragment : Fragment() {
    private lateinit var dbHelper: TravelDBHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_travel_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dbHelper = TravelDBHelper(requireContext())
        view.findViewById<TextView>(R.id.textTravelMapTitle).setText(R.string.map_title)
        view.findViewById<TextView>(R.id.textTravelMapDescription).setText(R.string.map_description)
    }

    override fun onResume() {
        super.onResume()
        loadLocations()
    }

    override fun onDestroyView() {
        dbHelper.close()
        super.onDestroyView()
    }

    private fun loadLocations() {
        val records = dbHelper.getTravelsWithLocation()
        val message = view?.findViewById<TextView>(R.id.textTravelMapMessage) ?: return
        val guide = view?.findViewById<TextView>(R.id.textTravelMapGuide) ?: return
        val marker = view?.findViewById<TextView>(R.id.textMapMarker) ?: return
        if (records.isEmpty()) {
            marker.setText(R.string.map_marker_empty)
            message.setText(R.string.map_empty_title)
            guide.setText(R.string.map_empty_message)
        } else {
            val latest = records.last()
            marker.text = records.size.toString()
            message.text = getString(R.string.map_count_message, records.size)
            guide.text = getString(R.string.map_latest_message, latest.place, latest.latitude, latest.longitude)
        }
    }
}
