package com.example.fti_barcodescannerapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fti_barcodescannerapp.databinding.ItemScanBinding
import com.example.fti_barcodescannerapp.domain.model.Scan
import java.text.SimpleDateFormat
import java.util.*

class ScanHistoryAdapter : RecyclerView.Adapter<ScanHistoryAdapter.VH>() {

    private val items = mutableListOf<Scan>()
    private val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    fun submitList(scans: List<Scan>) {
        items.clear()
        items.addAll(scans)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemScanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val scan = items[position]
        holder.binding.txtValue.text = scan.rawValue
        holder.binding.txtTime.text =
            formatter.format(Date(scan.timestampMs))
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemScanBinding) : RecyclerView.ViewHolder(binding.root)
}
