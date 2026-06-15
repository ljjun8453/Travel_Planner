package com.example.momentrip

import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.momentrip.databinding.FragmentTravelListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TravelListFragment : Fragment(), TravelAdapter.Listener {
    private var _binding: FragmentTravelListBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: TravelDBHelper
    private lateinit var adapter: TravelAdapter
    private var contextRecord: TravelRecord? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTravelListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dbHelper = TravelDBHelper(requireContext())

        binding.textTravelListTitle.setText(R.string.list_title)
        binding.textTravelListDescription.setText(R.string.list_description)
        binding.textTravelListMessage.setText(R.string.list_empty_title)
        binding.textTravelListGuide.setText(R.string.list_empty_message)

        binding.textTravelListIcon.setOnClickListener { openNewRecord() }
        binding.buttonAddRecordBottom.setOnClickListener { openNewRecord() }
        binding.buttonClearSelection.setOnClickListener { adapter.clearSelection() }
        binding.buttonDeleteSelected.setOnClickListener { confirmDeleteSelected() }

        adapter = TravelAdapter(mutableListOf(), this)
        binding.recyclerTravelRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTravelRecords.adapter = adapter
        binding.recyclerTravelRecords.isNestedScrollingEnabled = false
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
    }

    override fun onDestroyView() {
        dbHelper.close()
        _binding = null
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

            R.id.menuContextSelect -> {
                adapter.toggleSelection(record)
                true
            }

            R.id.menuContextDelete -> {
                confirmDelete(record)
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }

    override fun onSelectionChanged(count: Int) {
        val currentBinding = _binding ?: return
        currentBinding.selectionBar.visibility = if (count > 0) View.VISIBLE else View.GONE
        currentBinding.textSelectionCount.text = getString(R.string.selected_count, count)
    }

    private fun loadRecords() {
        val currentBinding = _binding ?: return

        currentBinding.progressTravelList.visibility = View.VISIBLE
        currentBinding.listEmptyBox.visibility = View.GONE
        currentBinding.recyclerTravelRecords.visibility = View.GONE
        currentBinding.buttonAddRecordBottom.visibility = View.GONE

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

            val latestBinding = _binding ?: return@launch
            if (!isAdded) return@launch

            latestBinding.progressTravelList.visibility = View.GONE

            if (result.second) {
                Toast.makeText(requireContext(), R.string.toast_load_failed, Toast.LENGTH_SHORT).show()
            }

            adapter.submitList(result.first)

            val isEmpty = result.first.isEmpty()
            latestBinding.listEmptyBox.visibility = if (isEmpty) View.VISIBLE else View.GONE
            latestBinding.recyclerTravelRecords.visibility = if (isEmpty) View.GONE else View.VISIBLE
            latestBinding.buttonAddRecordBottom.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun openNewRecord() {
        try {
            AddEditActivity.start(requireContext())
        } catch (_: Exception) {
            context?.let { Toast.makeText(it, R.string.error_screen_change, Toast.LENGTH_SHORT).show() }
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

    private fun deleteRecord(record: TravelRecord) {
        viewLifecycleOwner.lifecycleScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                try {
                    dbHelper.deleteTravel(record.no) > 0
                } catch (_: Exception) {
                    false
                }
            }

            if (!isAdded) return@launch

            if (deleted) {
                Toast.makeText(requireContext(), R.string.toast_deleted, Toast.LENGTH_SHORT).show()
                loadRecords()
            } else {
                Toast.makeText(requireContext(), R.string.toast_delete_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteSelected() {
        val selectedNumbers = adapter.getSelectedRecordNumbers()
        if (selectedNumbers.isEmpty()) return

        try {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.toast_selected_delete_count, selectedNumbers.size))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    deleteSelected(selectedNumbers)
                }
                .show()
        } catch (_: Exception) {
            context?.let { Toast.makeText(it, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun deleteSelected(numbers: List<Int>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) {
                try {
                    dbHelper.deleteTravels(numbers)
                } catch (_: Exception) {
                    0
                }
            }

            if (!isAdded) return@launch

            if (count > 0) {
                adapter.clearSelection()
                Toast.makeText(requireContext(), getString(R.string.toast_selected_delete_count, count), Toast.LENGTH_SHORT).show()
                loadRecords()
            } else {
                Toast.makeText(requireContext(), R.string.toast_delete_failed, Toast.LENGTH_SHORT).show()
            }
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