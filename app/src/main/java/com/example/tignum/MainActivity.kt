package com.example.tignum

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch


class MainActivity : FileDownloaderContract.View, AppCompatActivity() {

    private val permissionRequestCode = 1000
    private lateinit var presenter: FileDownloaderContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter = FileDownloaderPresenter(this, this)

        presenter.onViewCreated()


        startButton.setOnClickListener {
            presenter.startDownload()
        }

        pauseButton.setOnClickListener {
            presenter.pauseDownload()
        }

        deleteButton.setOnClickListener {
            presenter.deleteFile()
        }

        playButton.setOnClickListener {
            presenter.playFile()
        }

    }

    override fun showCurrentStatus(status: String) {
        CoroutineScope(Main).launch {
            currentStatusTextView.text = status
        }
    }

    override fun showErrorMessage(status: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(status)
        builder.setNeutralButton(android.R.string.yes) { dialog, which -> }
        builder.show()
    }

    override fun requestUserPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                &&
                checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            )
                presenter.onPermissionsReady()
            else
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    permissionRequestCode
                )
        } else
            presenter.onPermissionsReady()
    }

    override fun playMediaFile(fileUri: Uri) {
        val mediaController = MediaController(this);
        videoView.setVideoURI(fileUri)
        videoView.setMediaController(mediaController)
        videoView.start()
    }

    override fun enableButtons(map: HashMap<ButtonEnum, Boolean>) {
        for (button :ButtonEnum in map.keys) {
            when (button) {
                ButtonEnum.PAUSE -> pauseButton.isEnabled = map[button]!!
                ButtonEnum.PLAY -> pauseButton.isEnabled = map[button]!!
                ButtonEnum.DELETE -> pauseButton.isEnabled = map[button]!!
                ButtonEnum.START -> pauseButton.isEnabled = map[button]!!
            }
        }
    }

    override fun setProgressbarValue(value: Int) {
        progressBar.setProgress(value)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionRequestCode) {
            if ((grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED)
            ) {
                presenter.onPermissionsReady()
            } else {
                // todo add below stuff.
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
            }
        }
    }

}