package com.example.momentrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.momentrip.databinding.FragmentTravelPlanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TravelPlanFragment : Fragment(), PlanAdapter.Listener {
    private var _binding: FragmentTravelPlanBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: TravelDBHelper
    private lateinit var adapter: PlanAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTravelPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dbHelper = TravelDBHelper(requireContext())

        binding.buttonAddPlanIcon.setOnClickListener {
            openNewPlan()
        }

        binding.buttonAddPlanBottom.setOnClickListener {
            openNewPlan()
        }

        adapter = PlanAdapter(mutableListOf(), this)
        binding.recyclerTravelPlans.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTravelPlans.adapter = adapter
        binding.recyclerTravelPlans.isNestedScrollingEnabled = false
    }

    override fun onResume() {
        super.onResume()
        loadPlans()
    }

    override fun onDestroyView() {
        dbHelper.close()
        _binding = null
        super.onDestroyView()
    }

    override fun onPlanClick(plan: TravelPlan) {
        try {
            AddEditPlanActivity.start(requireContext(), plan.no)
        } catch (_: Exception) {
            context?.let {
                Toast.makeText(it, R.string.error_screen_change, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPlanLongClick(plan: TravelPlan) {
        showPlanMenu(plan)
    }

    private fun loadPlans() {
        val currentBinding = _binding ?: return

        currentBinding.progressTravelPlan.visibility = View.VISIBLE
        currentBinding.planEmptyBox.visibility = View.GONE
        currentBinding.recyclerTravelPlans.visibility = View.GONE
        currentBinding.buttonAddPlanBottom.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val plans = dbHelper.getAllPlans()

                    val sortedPlans = when (sortMode) {
                        SortMode.DEFAULT -> plans
                        SortMode.DATE -> plans.sortedBy { it.planDate }
                        SortMode.PLACE -> plans.sortedBy { it.place }
                    }

                    Pair(sortedPlans, false)
                } catch (_: Exception) {
                    Pair(emptyList<TravelPlan>(), true)
                }
            }

            val latestBinding = _binding ?: return@launch

            if (!isAdded) {
                return@launch
            }

            latestBinding.progressTravelPlan.visibility = View.GONE

            if (result.second) {
                Toast.makeText(requireContext(), R.string.toast_load_failed, Toast.LENGTH_SHORT).show()
            }

            adapter.submitList(result.first)

            val isEmpty = result.first.isEmpty()
            latestBinding.planEmptyBox.visibility = if (isEmpty) View.VISIBLE else View.GONE
            latestBinding.recyclerTravelPlans.visibility = if (isEmpty) View.GONE else View.VISIBLE
            latestBinding.buttonAddPlanBottom.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun openNewPlan() {
        try {
            AddEditPlanActivity.start(requireContext())
        } catch (_: Exception) {
            context?.let {
                Toast.makeText(it, R.string.error_screen_change, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPlanMenu(plan: TravelPlan) {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle(plan.place)
                .setItems(arrayOf("수정", "삭제")) { _, which ->
                    when (which) {
                        0 -> {
                            try {
                                AddEditPlanActivity.start(requireContext(), plan.no)
                            } catch (_: Exception) {
                                context?.let {
                                    Toast.makeText(it, R.string.error_screen_change, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        1 -> confirmDeletePlan(plan)
                    }
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        } catch (_: Exception) {
            context?.let {
                Toast.makeText(it, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeletePlan(plan: TravelPlan) {
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
            context?.let {
                Toast.makeText(it, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show()
            }
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
                Toast.makeText(requireContext(), R.string.toast_deleted, Toast.LENGTH_SHORT).show()
                loadPlans()
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