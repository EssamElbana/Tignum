package com.example.tignum.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tignum.R
import com.example.tignum.model.FileItem

private const val PAYLOAD_TAG_PROGRESS: Int = 50
private const val PAYLOAD_TAG_STATUS: Int = 100

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

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        when {
            payloads.isEmpty() -> super.onBindViewHolder(holder, position, payloads)
            else -> {
                val item: FileItem = listOfFiles[position]
                payloads.forEach {
                    when (it) {
                        PAYLOAD_TAG_PROGRESS -> holder.progressBarDownload.progress = item.progress
                        PAYLOAD_TAG_STATUS -> holder.textViewFileStatus.text = item.status
                    }
                }
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewFileName = itemView.findViewById(R.id.nameOfFile) as TextView
        val textViewFileStatus = itemView.findViewById(R.id.statusOfFile) as TextView
        val progressBarDownload = itemView.findViewById(R.id.progressBar) as ProgressBar

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
            progressBarDownload.progress = fileItem.progress
        }
    }


    interface ButtonsClickListener {
        fun deleteBtnClicked(position: Int)
        fun startBtnClicked(position: Int)
        fun pauseBtnClicked(position: Int)
        fun playBtnClicked(position: Int)
    }


}