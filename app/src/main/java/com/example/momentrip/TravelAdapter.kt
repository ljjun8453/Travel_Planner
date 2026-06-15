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
    interface Listener {
        fun onRecordClick(record: TravelRecord)
        fun onRecordLongClick(record: TravelRecord, anchor: View)
        fun onSelectionChanged(count: Int)
    }

    private val selectedRecordNumbers = linkedSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelViewHolder {
        val binding = ItemTravelRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TravelViewHolder(binding)
    }

    override fun getItemCount(): Int = records.size

    override fun onBindViewHolder(holder: TravelViewHolder, position: Int) {
        holder.bind(records[position])
    }

    fun submitList(items: List<TravelRecord>) {
        records.clear()
        records.addAll(items)
        selectedRecordNumbers.removeAll { selectedNo -> items.none { it.no == selectedNo } }
        notifyDataSetChanged()
        listener.onSelectionChanged(selectedRecordNumbers.size)
    }

    fun getSelectedRecordNumbers(): List<Int> {
        return selectedRecordNumbers.toList()
    }

    fun clearSelection() {
        selectedRecordNumbers.clear()
        notifyDataSetChanged()
        listener.onSelectionChanged(0)
    }

    fun toggleSelection(record: TravelRecord) {
        if (selectedRecordNumbers.contains(record.no)) {
            selectedRecordNumbers.remove(record.no)
        } else {
            selectedRecordNumbers.add(record.no)
        }
        notifyDataSetChanged()
        listener.onSelectionChanged(selectedRecordNumbers.size)
    }

    inner class TravelViewHolder(private val binding: ItemTravelRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: TravelRecord) {
            binding.itemTextPlace.text = record.place
            binding.itemTextDate.text = record.visitDate
            binding.itemTextMemo.text = record.memo ?: binding.root.context.getString(R.string.item_no_memo)
            binding.root.setBackgroundResource(if (selectedRecordNumbers.contains(record.no)) R.drawable.bg_fragment_card_selected else R.drawable.bg_fragment_card)
            try {
                val firstPhoto = TravelPhotoStore.first(record.photoUri)
                if (firstPhoto.isNullOrBlank()) {
                    binding.itemImagePhoto.setImageResource(R.drawable.ic_launcher_foreground)
                } else {
                    binding.itemImagePhoto.setImageURI(Uri.parse(firstPhoto))
                }
            } catch (_: Exception) {
                binding.itemImagePhoto.setImageResource(R.drawable.ic_launcher_foreground)
            }
            binding.root.setOnClickListener {
                if (selectedRecordNumbers.isEmpty()) {
                    listener.onRecordClick(record)
                } else {
                    toggleSelection(record)
                }
            }
            binding.root.setOnLongClickListener {
                if (selectedRecordNumbers.isEmpty()) {
                    listener.onRecordLongClick(record, binding.root)
                } else {
                    toggleSelection(record)
                }
                true
            }
        }
    }
}
