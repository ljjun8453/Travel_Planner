package com.example.momentrip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlanAdapter(
    private val plans: MutableList<TravelPlan>,
    private val listener: Listener
) : RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {
    interface Listener {
        fun onPlanLongClick(plan: TravelPlan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_travel_plan, parent, false)
        return PlanViewHolder(view)
    }

    override fun getItemCount(): Int = plans.size

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(plans[position])
    }

    fun submitList(items: List<TravelPlan>) {
        plans.clear()
        plans.addAll(items)
        notifyDataSetChanged()
    }

    inner class PlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textPlace: TextView = itemView.findViewById(R.id.itemPlanPlace)
        private val textDate: TextView = itemView.findViewById(R.id.itemPlanDate)
        private val textMemo: TextView = itemView.findViewById(R.id.itemPlanMemo)

        fun bind(plan: TravelPlan) {
            textPlace.text = plan.place
            textDate.text = plan.planDate
            textMemo.text = plan.memo ?: itemView.context.getString(R.string.item_no_memo)
            itemView.setOnLongClickListener {
                listener.onPlanLongClick(plan)
                true
            }
        }
    }
}
