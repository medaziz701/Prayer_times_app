package com.mosque.prayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mosque.prayer.R
import com.mosque.prayer.data.PrayerKey

data class PrayerRowUi(
    val key: PrayerKey,
    val name: String,
    val adhan: String,
    val iqama: String,
    val isNext: Boolean
)

class PrayerRowAdapter : RecyclerView.Adapter<PrayerRowAdapter.VH>() {

    private var items: List<PrayerRowUi> = emptyList()

    fun submit(newItems: List<PrayerRowUi>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_prayer_row, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val txtAdhan: TextView = itemView.findViewById(R.id.txtAdhan)
        private val txtIqama: TextView = itemView.findViewById(R.id.txtIqama)

        fun bind(item: PrayerRowUi) {
            txtName.text = item.name
            txtAdhan.text = item.adhan
            txtIqama.text = item.iqama

            if (item.isNext) {
                itemView.setBackgroundResource(R.drawable.bg_row_next)
                txtName.alpha = 1.0f
                txtAdhan.alpha = 1.0f
                txtIqama.alpha = 1.0f
                txtName.isSelected = true
                txtAdhan.isSelected = true
                txtIqama.isSelected = true
            } else {
                itemView.background = null
                txtName.alpha = 0.88f
                txtAdhan.alpha = 0.88f
                txtIqama.alpha = 0.88f
                txtName.isSelected = false
                txtAdhan.isSelected = false
                txtIqama.isSelected = false
            }
        }
    }
}
