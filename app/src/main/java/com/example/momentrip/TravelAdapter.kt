package com.example.momentrip

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.momentrip.databinding.ItemTravelRecordBinding

class TravelAdapter(
    private val records: MutableList<TravelRecord>,
    private val listener: Listener
) : RecyclerView.Adapter<TravelAdapter.TravelViewHolder>() {

    private val selectedNumbers = mutableSetOf<Int>()

    interface Listener {
        fun onRecordClick(record: TravelRecord)
        fun onRecordLongClick(record: TravelRecord, anchor: View)
        fun onSelectionChanged(count: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelViewHolder {
        val binding = ItemTravelRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return TravelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TravelViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int {
        return records.size
    }

    fun submitList(newRecords: List<TravelRecord>) {
        records.clear()
        records.addAll(newRecords)
        selectedNumbers.clear()

        notifyDataSetChanged()
        listener.onSelectionChanged(selectedNumbers.size)
    }

    fun toggleSelection(record: TravelRecord) {
        if (selectedNumbers.contains(record.no)) {
            selectedNumbers.remove(record.no)
        } else {
            selectedNumbers.add(record.no)
        }

        notifyDataSetChanged()
        listener.onSelectionChanged(selectedNumbers.size)
    }

    fun clearSelection() {
        selectedNumbers.clear()
        notifyDataSetChanged()
        listener.onSelectionChanged(selectedNumbers.size)
    }

    fun getSelectedRecordNumbers(): List<Int> {
        return selectedNumbers.toList()
    }

    inner class TravelViewHolder(
        private val binding: ItemTravelRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: TravelRecord) {
            binding.itemTextPlace.text = record.place
            binding.itemTextDate.text = record.visitDate

            binding.itemTextMemo.text = if (record.memo.isNullOrBlank()) {
                itemView.context.getString(R.string.item_no_memo)
            } else {
                record.memo
            }

            setRecordImage(record)

            binding.root.isSelected = selectedNumbers.contains(record.no)

            binding.root.setOnClickListener {
                listener.onRecordClick(record)
            }

            binding.root.setOnLongClickListener {
                listener.onRecordLongClick(record, binding.root)
                true
            }
        }

        private fun setRecordImage(record: TravelRecord) {
            try {
                if (record.photoUri.isNullOrBlank()) {
                    binding.itemImagePhoto.setImageResource(R.drawable.momentrip1)
                } else {
                    binding.itemImagePhoto.setImageURI(Uri.parse(record.photoUri))
                }
            } catch (_: Exception) {
                binding.itemImagePhoto.setImageResource(R.drawable.momentrip1)
            }
        }
    }
}