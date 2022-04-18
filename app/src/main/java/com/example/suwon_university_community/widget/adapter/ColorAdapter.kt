package com.example.suwon_university_community.widget.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.suwon_university_community.databinding.ViewholderColorBinding

class ColorAdapter(
    private val colorIdList: List<Int>,
    private val itemCLickListener: (Int) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder =
        ColorViewHolder(
            ViewholderColorBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colorIdList[position])
    }

    override fun getItemCount(): Int = colorIdList.size

    inner class ColorViewHolder(private val binding: ViewholderColorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.colorButton.setOnClickListener {
                itemCLickListener.invoke(colorIdList[adapterPosition])
            }
        }

        fun bind(colorId: Int) {
            binding.colorButton.apply {
                setBackgroundColor(ContextCompat.getColor(context, colorId))
            }
        }
    }
}