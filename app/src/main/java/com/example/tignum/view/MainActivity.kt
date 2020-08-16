package com.example.tignum.view

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tignum.R
import com.example.tignum.repo.Repository
import com.example.tignum.model.FileItem
import kotlinx.android.synthetic.main.activity_main.*


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
        presenter = FileDownloaderPresenter(this,
            Repository(this)
        )

        adapter.listOfFiles = ArrayList()
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
        builder.setNeutralButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun confirmUserDeletion(position: Int, fileName: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure you want to delete $fileName? all file progress will be lose.")
        builder.setPositiveButton(android.R.string.yes) { dialog, _ ->
            presenter.onDeletionConfirmed(position, fileName)
            dialog.dismiss()
        }
        builder.setNegativeButton(android.R.string.no) { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun playMediaFile(filePath: String) {
        intent = Intent(this, PlayVideoActivity::class.java)
        intent.putExtra("FILE_PATH", filePath)
        startActivity(intent)
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
                showErrorMessage("Application won't be able to download files until permissions is granted!")
            }
        }
    }

    override fun deleteBtnClicked(position: Int) = presenter.deleteBtnClicked(position)

    override fun startBtnClicked(position: Int) = presenter.startBtnClicked(position)

    override fun pauseBtnClicked(position: Int) = presenter.pauseBtnClicked(position)

    override fun playBtnClicked(position: Int) = presenter.playBtnClicked(position)

    override fun setItemChanged(position: Int, payload: Int?) =
        adapter.notifyItemChanged(position, payload)

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}