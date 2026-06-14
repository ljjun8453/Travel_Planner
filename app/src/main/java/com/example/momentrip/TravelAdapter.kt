package com.example.momentrip

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TravelAdapter(
    private val records: MutableList<TravelRecord>,
    private val listener: Listener
) : RecyclerView.Adapter<TravelAdapter.TravelViewHolder>() {
    interface Listener {
        fun onRecordClick(record: TravelRecord)
        fun onRecordLongClick(record: TravelRecord, anchor: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_travel_record, parent, false)
        return TravelViewHolder(view)
    }

    override fun getItemCount(): Int = records.size

    override fun onBindViewHolder(holder: TravelViewHolder, position: Int) {
        holder.bind(records[position])
    }

    fun submitList(items: List<TravelRecord>) {
        records.clear()
        records.addAll(items)
        notifyDataSetChanged()
    }

    inner class TravelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagePhoto: ImageView = itemView.findViewById(R.id.itemImagePhoto)
        private val textPlace: TextView = itemView.findViewById(R.id.itemTextPlace)
        private val textDate: TextView = itemView.findViewById(R.id.itemTextDate)
        private val textMemo: TextView = itemView.findViewById(R.id.itemTextMemo)

        fun bind(record: TravelRecord) {
            textPlace.text = record.place
            textDate.text = record.visitDate
            textMemo.text = record.memo ?: "메모 없음"
            if (record.photoUri.isNullOrBlank()) {
                imagePhoto.setImageResource(R.drawable.ic_launcher_foreground)
            } else {
                imagePhoto.setImageURI(Uri.parse(record.photoUri))
            }
            itemView.setOnClickListener { listener.onRecordClick(record) }
            itemView.setOnLongClickListener {
                listener.onRecordLongClick(record, itemView)
                true
            }
        }
    }
}
