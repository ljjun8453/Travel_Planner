package com.example.momentrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.momentrip.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonHomeAdd.setOnClickListener {
            showCreateDialog()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun showCreateDialog() {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_create_title)
                .setItems(arrayOf(getString(R.string.dialog_create_record), getString(R.string.dialog_create_plan))) { _, which ->
                    if (which == 0) {
                        AddEditActivity.start(requireContext())
                    } else {
                        AddEditPlanActivity.start(requireContext())
                    }
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        } catch (_: Exception) {
            context?.let { Toast.makeText(it, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show() }
        }
    }
}
