package com.example.momentrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.buttonHomeAdd).setOnClickListener {
            showCreateDialog()
        }
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
                .show()
        } catch (_: Exception) {
            context?.let { Toast.makeText(it, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show() }
        }
    }
}
