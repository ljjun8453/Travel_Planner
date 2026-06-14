package com.example.momentrip

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

class TravelListFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            inflater.inflate(R.layout.fragment_travel_list, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "onCreateView error", e)
            Toast.makeText(requireContext(), "목록 화면을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    companion object {
        private const val TAG = "TravelListFragment"
    }
}
