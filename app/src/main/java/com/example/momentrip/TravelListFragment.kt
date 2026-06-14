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
        view.findViewById<TextView>(R.id.textTravelListTitle).setText(R.string.list_title)
        view.findViewById<TextView>(R.id.textTravelListDescription).setText(R.string.list_description)
        view.findViewById<TextView>(R.id.textTravelListMessage).setText(R.string.list_empty_title)
        view.findViewById<TextView>(R.id.textTravelListGuide).setText(R.string.list_empty_message)
        view.findViewById<Button>(R.id.buttonAddFirst).setOnClickListener { AddEditActivity.start(requireContext()) }
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
        DetailActivity.start(requireContext(), record.no)
    }

    override fun onRecordLongClick(record: TravelRecord, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, MENU_EDIT, 0, getString(R.string.action_edit))
        popup.menu.add(0, MENU_DELETE, 1, getString(R.string.action_delete))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_EDIT -> AddEditActivity.start(requireContext(), record.no)
                MENU_DELETE -> confirmDelete(record)
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
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, record.place))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                if (dbHelper.deleteTravel(record.no) > 0) {
                    Toast.makeText(requireContext(), R.string.toast_deleted, Toast.LENGTH_SHORT).show()
                    loadRecords()
                } else {
                    Toast.makeText(requireContext(), R.string.toast_delete_failed, Toast.LENGTH_SHORT).show()
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
        private const val MENU_EDIT = 1
        private const val MENU_DELETE = 2
        var sortMode = SortMode.DEFAULT
    }
}
