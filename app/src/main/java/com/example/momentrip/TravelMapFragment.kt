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
        view.findViewById<TextView>(R.id.textTravelMapTitle).text = "지도 보기"
        view.findViewById<TextView>(R.id.textTravelMapDescription).text = "좌표가 저장된 여행 기록을 지도 패널에서 확인하세요."
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
            marker.text = "◎"
            message.text = "위치가 저장된 여행 기록이 없습니다."
            guide.text = "기록 추가 화면에서 위도와 경도를 입력하면 이곳에 표시됩니다."
        } else {
            val latest = records.last()
            marker.text = records.size.toString()
            message.text = "${records.size}개 장소가 저장되어 있습니다."
            guide.text = "${latest.place} · ${latest.latitude}, ${latest.longitude}"
        }
    }
}
