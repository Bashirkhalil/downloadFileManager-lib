## :slightly_smiling_face:  downloadFileManager-lib 

[![](https://jitpack.io/v/Bashirkhalil/downloadFileManager-lib.svg)](https://jitpack.io/#Bashirkhalil/downloadFileManager-lib)


## Features 

1. set your custom directory otherwise default will be the download directory .
2. set your custom name otherwise will be the "DefaultName" .
3. set you extension file [ apk - png - jpeg - pdf - pptx ] .
4. mointor you download progress and customized with your best way such as show user progress .


	
	
## Gradle [ setting or project  Level ]


	allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}

## Gradle [   Module Level ]
	
	
     dependencies  {
      implementation 'com.github.Bashirkhalil:downloadFileManager-lib:v2.1.0'
     }
	

## Example 

      var  url ="http://shouji.360tpcdn.com/170918/93d1695d87df5a0c0002058afc0361f1/com.ss.android.article.news_636.apk"

        var dir = File(Environment.getExternalStorageDirectory().toString() + "/${Environment.DIRECTORY_DOWNLOADS}/AHT/")
        var download = DownloadFile()
        download.setBaseURl("https://apkfab.com/upload/apk/")
        download.setUrl(url)
        download.setExtension(".apk")
        download.setDir(dir)               // option
        download.setFileName(appName)     // option
        download.setOnDownloadListener(object : DownloadFile.OnDownloadListener {

            override fun onFileName(fileName: String) {
                Log.e(mTag, "fileName -> $fileName")
                fileNameIs = fileName
            }

            override fun onStart() {
                Log.e(mTag, "${Thread.currentThread()} execution  is start ")
                Helper.progressDialog(mContext, "请稍等", "请等待文件正在下载...")
            }

            override fun onFinish() {
                Log.e(mTag, "${Thread.currentThread()} execution  is finish ")

            }

            override fun onError(message: String) {
                Log.e(mTag, "${Thread.currentThread()} onError -> $message")
                Helper.progressDialogDismiss()
            }

            override fun onNoInternetConnect(status: Boolean, message: String) {
                Log.e(
                    mTag,
                    "${Thread.currentThread()} onNoInternetConnect -> $status  - message -> $message"
                )
                Helper.noInternetConnection(mContext)
            }

            override fun onDownloadSuccess(status: Boolean) {
                runOnUiThread {
                    Log.e(mTag, "${Thread.currentThread()} onDownloadSuccess -> $status")
                    Helper.progressDialogDismiss()
                    showOpenFolderDialog()
                }
            }

            override fun onDownloadFiled(status1: String?, status: Boolean) {
                Log.e(mTag, "${Thread.currentThread()} onDownloadFiled -> $status")
                Helper.progressDialogDismiss()
            }

            override fun onDownloadProgress(fileSize: Long, fileSizeDownloaded: Long, value: Int) {
                // value = (((fileSizeDownloaded * 100) / fileSize).toInt()
                Log.e(mTag, "fileSize =$fileSize  - file download=$fileSizeDownloaded of $fileSize -  ${fileSizeDownloaded / 1024} MB value=$value ")
            }

            override fun onPermissionResult(permssion: Boolean, message: String) {
                Log.e(
                    mTag,
                    "${Thread.currentThread()} onPermissionResult -> $permssion  - message -> $message"
                )
            }

        })
        download.build(this)

