package com.example.momentrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TravelListFragment : Fragment(), TravelAdapter.Listener {
    private lateinit var dbHelper: TravelDBHelper
    private lateinit var adapter: TravelAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyBox: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_travel_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dbHelper = TravelDBHelper(requireContext())
        view.findViewById<TextView>(R.id.textTravelListTitle).text = "여행 기록 목록"
        view.findViewById<TextView>(R.id.textTravelListDescription).text = "방문한 장소, 날짜, 사진과 메모를 한눈에 확인하세요."
        view.findViewById<TextView>(R.id.textTravelListMessage).text = "저장된 여행 기록이 없습니다."
        view.findViewById<TextView>(R.id.textTravelListGuide).text = "오른쪽 위 메뉴에서 새 기록을 추가하세요."
        view.findViewById<Button>(R.id.buttonAddFirst).setOnClickListener { TravelEditActivity.start(requireContext()) }
        emptyBox = view.findViewById(R.id.listEmptyBox)
        recyclerView = view.findViewById(R.id.recyclerTravelRecords)
        adapter = TravelAdapter(mutableListOf(), this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
    }

    override fun onDestroyView() {
        dbHelper.close()
        super.onDestroyView()
    }

    override fun onRecordClick(record: TravelRecord) {
        TravelEditActivity.start(requireContext(), record.no)
    }

    override fun onRecordLongClick(record: TravelRecord, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add("수정")
        popup.menu.add("삭제")
        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "수정" -> TravelEditActivity.start(requireContext(), record.no)
                "삭제" -> confirmDelete(record)
            }
            true
        }
        popup.show()
    }

    private fun loadRecords() {
        val records = when (sortMode) {
            SortMode.DATE -> dbHelper.getAllTravelsOrderByDate()
            SortMode.PLACE -> dbHelper.getAllTravelsOrderByPlace()
            SortMode.DEFAULT -> dbHelper.getAllTravels()
        }
        adapter.submitList(records)
        emptyBox.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun confirmDelete(record: TravelRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("기록 삭제")
            .setMessage("${record.place} 기록을 삭제할까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                if (dbHelper.deleteTravel(record.no) > 0) {
                    Toast.makeText(requireContext(), "삭제했습니다.", Toast.LENGTH_SHORT).show()
                    loadRecords()
                } else {
                    Toast.makeText(requireContext(), "삭제하지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    enum class SortMode {
        DEFAULT,
        DATE,
        PLACE
    }

    companion object {
        var sortMode = SortMode.DEFAULT
    }
}
