package com.example.tignum.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tignum.R

class FilesRecyclerViewAdapter(val listener: ButtonsClickListener) :
    RecyclerView.Adapter<FilesRecyclerViewAdapter.ViewHolder>() {
    lateinit var listOfFiles: ArrayList<FileItem>
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_file_operations, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = listOfFiles.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(listOfFiles[position], position, listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewFileName = itemView.findViewById(R.id.nameOfFile) as TextView
        private val textViewFileStatus = itemView.findViewById(R.id.statusOfFile) as TextView

        fun bindItems(fileItem: FileItem, position: Int, listener: ButtonsClickListener) {

            val startBtn = itemView.findViewById<Button>(R.id.startButton)
            startBtn.setOnClickListener { listener.startBtnClicked(position) }

            val pauseBtn = itemView.findViewById<Button>(R.id.pauseDownloadButton)
            pauseBtn.setOnClickListener { listener.pauseBtnClicked(position) }

            val deleteBtn = itemView.findViewById<Button>(R.id.deleteFileButton)
            deleteBtn.setOnClickListener { listener.deleteBtnClicked(position) }

            val playBtn = itemView.findViewById<Button>(R.id.playFileButton)
            playBtn.setOnClickListener { listener.playBtnClicked(position) }

            textViewFileName.text = fileItem.fileName
            textViewFileStatus.text = fileItem.status
        }
    }

    interface ButtonsClickListener {
        fun deleteBtnClicked(position: Int)
        fun startBtnClicked(position: Int)
        fun pauseBtnClicked(position: Int)
        fun playBtnClicked(position: Int)
    }


}