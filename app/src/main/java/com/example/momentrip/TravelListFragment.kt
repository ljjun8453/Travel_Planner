package com.example.momentrip

import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TravelListFragment : Fragment(), TravelAdapter.Listener {
    private lateinit var dbHelper: TravelDBHelper
    private lateinit var adapter: TravelAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyBox: View
    private lateinit var progressBar: ProgressBar
    private var contextRecord: TravelRecord? = null

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
        progressBar = view.findViewById(R.id.progressTravelList)
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
        try {
            DetailActivity.start(requireContext(), record.no)
        } catch (_: Exception) {
            context?.let { Toast.makeText(it, R.string.error_screen_change, Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onRecordLongClick(record: TravelRecord, anchor: View) {
        try {
            contextRecord = record
            registerForContextMenu(anchor)
            anchor.showContextMenu()
        } catch (_: Exception) {
            context?.let { Toast.makeText(it, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.setHeaderTitle(contextRecord?.place ?: getString(R.string.list_title))
        requireActivity().menuInflater.inflate(R.menu.menu_travel_context, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val record = contextRecord ?: return super.onContextItemSelected(item)
        return when (item.itemId) {
            R.id.menuContextEdit -> {
                try {
                    AddEditActivity.start(requireContext(), record.no)
                } catch (_: Exception) {
                    context?.let { Toast.makeText(it, R.string.error_screen_change, Toast.LENGTH_SHORT).show() }
                }
                true
            }
            R.id.menuContextDelete -> {
                confirmDelete(record)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun loadRecords() {
        progressBar.visibility = View.VISIBLE
        emptyBox.visibility = View.GONE
        recyclerView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Pair(
                        when (sortMode) {
                            SortMode.DATE -> dbHelper.getAllTravelsOrderByDate()
                            SortMode.PLACE -> dbHelper.getAllTravelsOrderByPlace()
                            SortMode.DEFAULT -> dbHelper.getAllTravels()
                        },
                        false
                    )
                } catch (_: Exception) {
                    Pair(emptyList<TravelRecord>(), true)
                }
            }

            if (!isAdded) {
                return@launch
            }
            progressBar.visibility = View.GONE
            if (result.second) {
                Toast.makeText(requireContext(), R.string.toast_load_failed, Toast.LENGTH_SHORT).show()
            }
            adapter.submitList(result.first)
            emptyBox.visibility = if (result.first.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (result.first.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun deleteRecord(record: TravelRecord) {
        viewLifecycleOwner.lifecycleScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                try {
                    dbHelper.deleteTravel(record.no) > 0
                } catch (_: Exception) {
                    false
                }
            }

            if (!isAdded) {
                return@launch
            }
            if (deleted) {
                Toast.makeText(requireContext(), R.string.toast_deleted, Toast.LENGTH_SHORT).show()
                loadRecords()
            } else {
                Toast.makeText(requireContext(), R.string.toast_delete_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(record: TravelRecord) {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, record.place))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    deleteRecord(record)
                }
                .show()
        } catch (_: Exception) {
            context?.let { Toast.makeText(it, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show() }
        }
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
