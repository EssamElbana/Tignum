package com.example.tignum

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tignum.view.FileItem
import com.example.tignum.view.FilesRecyclerViewAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch


class MainActivity : FileDownloaderContract.View, AppCompatActivity(),
    FilesRecyclerViewAdapter.ButtonsClickListener {

    private val permissionRequestCode = 1000
    private lateinit var presenter: FileDownloaderContract.Presenter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val adapter = FilesRecyclerViewAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        presenter = FileDownloaderPresenter(this, this)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)

        presenter.onViewCreated()

        enqueueBtn.setOnClickListener {
            presenter.enqueueBtnClicked()
        }
    }

    override fun showErrorMessage(status: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(status)
        builder.setNeutralButton(android.R.string.yes) { dialog, which -> }
        builder.show()
    }

    override fun playMediaFile(fileUri: Uri) {
//        val mediaController = MediaController(this);
//        videoView.setVideoURI(fileUri)
//        videoView.setMediaController(mediaController)
//        videoView.start()
    }


    override fun getInputUrl() = urlEditTextLayout.editText!!.text.toString()

    override fun showFileList(fileList: ArrayList<FileItem>) {
        adapter.listOfFiles = fileList
        adapter.notifyDataSetChanged()
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
                showErrorMessage("Application won't be able to download files!")
            }
        }
    }

    override fun deleteBtnClicked(position: Int) = presenter.delete(position)

    override fun startBtnClicked(position: Int) = presenter.start(position)

    override fun pauseBtnClicked(position: Int) = presenter.pause(position)

    override fun playBtnClicked(position: Int) = presenter.play(position)

    override fun setCurrentProgress(value: Int, readSoFar:Long, totalLength:Long) {
        CoroutineScope(Main).launch {
            if(value >= 0)
                progressBar.progress = value
            else
                progressBar.progress = 0
            if(totalLength <= 0)
                progressOfFile.text = "$readSoFar bytes / total is unknown from server"
            else
                progressOfFile.text = "$readSoFar / $totalLength bytes"
        }
    }

    override fun setItemChanged(position: Int) = adapter.notifyItemChanged(position)

}