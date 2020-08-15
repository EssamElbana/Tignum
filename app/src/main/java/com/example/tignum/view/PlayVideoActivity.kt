package com.example.tignum.view

import android.app.AlertDialog
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.tignum.R
import kotlinx.android.synthetic.main.activity_video_player.*

class PlayVideoActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        val filePath = intent.getStringExtra("FILE_PATH")
        val mediaController = MediaController(this);

        if(filePath != null) {
            videoView.setVideoURI(filePath.toUri())
            videoView.setMediaController(mediaController)
            videoView.start()
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Couldn't Locate File")
            builder.setNeutralButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            builder.show()
        }
    }
}