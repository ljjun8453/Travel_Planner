package com.example.momentrip

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.momentrip.databinding.ItemTravelPlanBinding

class PlanAdapter(
    private val plans: MutableList<TravelPlan>,
    private val listener: Listener
) : RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {
    interface Listener {
        fun onPlanClick(plan: TravelPlan)
        fun onPlanLongClick(plan: TravelPlan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemTravelPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlanViewHolder(binding)
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

    inner class PlanViewHolder(private val binding: ItemTravelPlanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plan: TravelPlan) {
            binding.itemPlanPlace.text = plan.place
            binding.itemPlanDate.text = plan.planDate
            binding.itemPlanMemo.text = plan.memo ?: binding.root.context.getString(R.string.item_no_memo)
            binding.root.setOnClickListener {
                listener.onPlanClick(plan)
            }
            binding.root.setOnLongClickListener {
                listener.onPlanLongClick(plan)
                true
            }
        }
    }
}
