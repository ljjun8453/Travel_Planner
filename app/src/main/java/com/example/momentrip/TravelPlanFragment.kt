package com.example.momentrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TravelPlanFragment : Fragment(), PlanAdapter.Listener {
    private lateinit var dbHelper: TravelDBHelper
    private lateinit var adapter: PlanAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyBox: View
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_travel_plan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dbHelper = TravelDBHelper(requireContext())
        emptyBox = view.findViewById(R.id.planEmptyBox)
        progressBar = view.findViewById(R.id.progressTravelPlan)
        recyclerView = view.findViewById(R.id.recyclerTravelPlans)
        adapter = PlanAdapter(mutableListOf(), this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        view.findViewById<Button>(R.id.buttonAddPlanFirst).setOnClickListener {
            AddEditPlanActivity.start(requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        loadPlans()
    }

    override fun onDestroyView() {
        dbHelper.close()
        super.onDestroyView()
    }

    override fun onPlanLongClick(plan: TravelPlan) {
        confirmDelete(plan)
    }

    private fun loadPlans() {
        progressBar.visibility = View.VISIBLE
        emptyBox.visibility = View.GONE
        recyclerView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Pair(dbHelper.getAllPlans(), false)
                } catch (_: Exception) {
                    Pair(emptyList<TravelPlan>(), true)
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

    private fun confirmDelete(plan: TravelPlan) {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, plan.place))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    deletePlan(plan)
                }
                .show()
        } catch (_: Exception) {
            context?.let { Toast.makeText(it, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun deletePlan(plan: TravelPlan) {
        viewLifecycleOwner.lifecycleScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                try {
                    dbHelper.deletePlan(plan.no) > 0
                } catch (_: Exception) {
                    false
                }
            }

            if (!isAdded) {
                return@launch
            }
            if (deleted) {
                Toast.makeText(requireContext(), R.string.toast_plan_deleted, Toast.LENGTH_SHORT).show()
                loadPlans()
            } else {
                Toast.makeText(requireContext(), R.string.toast_plan_delete_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
