package com.bk.downloadfileproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.bk.downloadfilemanager.DownloadFile
import java.io.File

class MainActivity : AppCompatActivity() {

    private var mTag = MainActivity::class.java.simpleName
    private var fileNameIs = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//       var  url ="https://s3-us-west-2.amazonaws.com/uw-s3-cdn/wp-content/uploads/sites/6/2017/11/04133712/waterfall.jpg"
//       var  url ="http://shouji.360tpcdn.com/170918/93d1695d87df5a0c0002058afc0361f1/com.ss.android.article.news_636.apk"
        var  url ="http://shouji.360tpcdn.com/170918/f7aa8587561e4031553316ada312ab38/com.tencent.qqlive_13049.apk"
//       var  url ="https://cdn.apedesign.net/github/logo.png"
//        var url ="https://www.kutub.info/downloadBook?book_id=eyJpdiI6IjFjZnl1Yko3QW5Qb0FDZ0Q4UHVrTUE9PSIsInZhbHVlIjoiVnRoaXR0WVJ3WjlZQWgyZzlWNUJhMVRGRE5SRWd5S3dndkRpVWRNZ1hjbkVXRFdBSVl0RUp3ZWZJL2htSVJ4TjRaV0xnczBIeFJQQStuQmt5VlJ6dmRWaHIvUmlJTGhrVkZIQ0ZxbnpqaWc9IiwibWFjIjoiOGQyMWMzMjE0ODk2NWQ3YmQ3NjdhNTlhNjBmNWNhNjgxODM4ZTE1Njk3ZGI4MmJmODVhMjc4ODRiMDA5MTc0ZCJ9"
//       val dir = File("${getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}/KFC/")

        var appName = "newFiles"

        var dir = File(Environment.getExternalStorageDirectory().toString() + "/${Environment.DIRECTORY_DOWNLOADS}/AHT/")
        var download = DownloadFile()
        download.setBaseURl("https://apkfab.com/upload/apk/")
        download.setUrl(url)
        download.setExtension(".apk")
        download.setDir(dir)   // option
        download.setFileName(appName) // option
        download.build(this)
        download.setOnDownloadListener(object : DownloadFile.OnDownloadListener {

            override fun onFileName(fileName: String) {
                Log.e(mTag, "fileName -> $fileName")
                fileNameIs = fileName
            }

            override fun onStart() {
                Log.e(mTag, "${Thread.currentThread()} execution  is start ")
//                Helper.progressDialog(mContext, "请稍等", "请等待文件正在下载...")
            }

            override fun onFinish() {
                Log.e(mTag, "${Thread.currentThread()} execution  is finish ")

            }

            override fun onError(message: String) {
                Log.e(mTag, "${Thread.currentThread()} onError -> $message")
//                Helper.progressDialogDismiss()
            }

            override fun onNoInternetConnect(status: Boolean, message: String) {
                Log.e(
                    mTag,
                    "${Thread.currentThread()} onNoInternetConnect -> $status  - message -> $message"
                )
//                Helper.noInternetConnection(mContext)
            }

            override fun onDownloadSuccess(status: Boolean) {
                runOnUiThread {
                    Log.e(mTag, "${Thread.currentThread()} onDownloadSuccess -> $status")
//                    Helper.progressDialogDismiss()
//                    showOpenFolderDialog()
                }
            }

            override fun onDownloadProgress(fileSize: Long, fileSizeDownloaded: Long, value: Int) {
                Log.e(mTag, "file download=$fileSizeDownloaded of $fileSize -${fileSizeDownloaded / 1024} MB value=$value ")
            }

            override fun onDownloadFiled(status1: String?, status: Boolean) {
                Log.e(mTag, "${Thread.currentThread()} onDownloadFiled -> $status")
//                Helper.progressDialogDismiss()
            }

            override fun onPermissionResult(permssion: Boolean, message: String) {
                Log.e(
                    mTag,
                    "${Thread.currentThread()} onPermissionResult -> $permssion  - message -> $message"
                )
            }

        })


    }
}