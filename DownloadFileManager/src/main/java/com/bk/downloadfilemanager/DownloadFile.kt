package com.bk.downloadfilemanager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import java.io.*


class DownloadFile {

    /* Begin of class [in the name of God the most ]*/
    private var mTag = DownloadFile::class.java.simpleName
    lateinit var mContext: Context
    private var mJob: Job? = null

    private var mBaseURL: String? = null
    private var mURL: String? = null
    private var extension: String? = null
    private var mDire: File? = null
    private var mAppName: String? = null
    private var mOriginalFileName: String?=null

    private var mOnItemResultListener: OnDownloadListener? = null
    private val coroutineExceptionHandler = CoroutineExceptionHandler { context, throwable ->
        Log.e(
            mTag,
            "coroutineExceptionHandler -> ${throwable.message} - ${throwable.printStackTrace()}"
        )
        getListener()?.onDownloadFiled(throwable.message, false)
    }

    /***
     ***  Main Function will be declare it here
     ***   this one is primary for evey use
     ***/
    fun setBaseURl(BaseURL: String) {
        mBaseURL = BaseURL
    }

    fun setUrl(url: String) {
        this.mURL = url
    }

    fun setExtension(extension: String) {
        this.extension = extension
    }


    /**
     ***  Option Function will be declare it here
     ***   on the other hand this one is option
     **/

    fun setDir(mFile: File) {
        this.mDire = mFile
    }

    fun setFileName(fileName: String) {
        mAppName = fileName
    }

    /***
     *** execution will start from builder method
     ***/
    fun build(context: Context) {

        mContext = context

        // check Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                mContext.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                getListener()?.onPermissionResult(
                    false,
                    "Plz Grand permission in order to download file !!!"
                )
                return
            }
        }

        // check if internet is connection
        if (!isConnect(mContext)) {
            mOnItemResultListener?.onNoInternetConnect(false, "Plz check Internet connection !!!")
            return
        }

        // set BaseUrl
        if (mBaseURL.isNullOrEmpty()) {
            mOnItemResultListener?.onError("Please enter the BaseUrl Eg : https://apkfab.com/upload/apk/ ")
            return
        }

        // check if url is null then return false
        if (mURL.isNullOrEmpty()) {
            mOnItemResultListener?.onError("Please enter the mURL Eg : courses/archive/spr11/cos217/lectures/08DsAlg.pdf ")
            return
        }

        // check if extensions is empty
        if (extension.isNullOrEmpty()) {
            mOnItemResultListener?.onError("Please set the file extensions  pdf - png - jpeg - apk ... etc ")
            return
        }


        /**
         ** check option function
         **/
        // if it's null the default will be download folder
        if (mDire == null) {
            mDire = File(
                Environment.getExternalStorageDirectory()
                    .toString() + "/${Environment.DIRECTORY_DOWNLOADS}"
            )
        }

        // if it's null the default will be defaultName name
        if (mAppName == null) {
            mAppName = "defaultName"
        }


        iniDownloading()

    }

    private fun iniDownloading() {

        getListener()?.onStart()
        mJob = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            val httpClient = OkHttpClient.Builder()
            val retrofit = Retrofit
                .Builder()
                .baseUrl(mBaseURL)
                .client(httpClient.build())
                .build()
            var mApiServices = retrofit.create(ApiServices::class.java)
            var responseResult = mApiServices.downloadFileByUrl(mURL)

//            Log.e(mTag, "Response is 01 -> ${responseResult.raw().message}")
//            Log.e(mTag, "Response is 02 -> ${responseResult.raw().request.url}")
//            Log.e(mTag, "Response is 1 -> ${responseResult.body()} ")
//            Log.e(mTag, "Response is 2 -> ${responseResult.errorBody()} ")
//            Log.e(mTag, "Response is 3 -> ${responseResult.headers()} ")
//            Log.e(mTag, "Response is 3 -> ${responseResult.code()} ")
//

            if (responseResult.isSuccessful) {

                /*** if the name is null then set default name */
                iniFileName(responseResult)

                /*** check the response body */
                responseResult.body()?.let {
//                    val writtenToDisk = writeResponseBodyToDiskAndDeployment(it, appName)
                    val writtenToDisk = writeResponseBody(it)

                    // return the response on Main Thread
                    withContext(Dispatchers.Main) {
                        if (writtenToDisk) {
                            getListener()?.onFinish()
                            getListener()?.onDownloadSuccess(true)
                        } else {
                            getListener()?.onDownloadFiled(responseResult.message(), false)
                        }
                    }
                }

            } else {
                onError("${responseResult.message()} -  ${responseResult.code()}")
            }
        }

    }

    /***
     *** Helper function
     ***/

    private fun isConnect(mContext: Context): Boolean {
        val connectivityManager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
        return false
    }

    private fun iniFileName(responseResult: Response<ResponseBody>) {
        try {
            getFileNameFromHeaders(responseResult).also {
                mOriginalFileName = it.substring(0, it.length - 4)
                if (mAppName.isNullOrEmpty()) {
                    mAppName = mOriginalFileName
                }
            }
        } catch (e: Exception) {
            Log.e(mTag, "Exception occur to get Original name $e ")
        }

        Log.e(mTag, "originalFileName = $mOriginalFileName  - mAppName = $mAppName")

    }

    private fun onError(message: String) {
        Log.e(mTag, "$message")

        (mContext as Activity).runOnUiThread {
            getListener()?.onError(message)
        }
    }

    private fun getFileNameFromHeaders(responseResult: Response<ResponseBody>): String {
        var content = responseResult.headers()["Content-Disposition"].toString()
//        Log.e(mTag, "content is $content ")
        var fullName = StringBuffer()
        for (i in 22 until content.length - 1) {
            fullName.append(content[i])
        }
        return fullName.toString()

    }

    private fun getNewFileName(apkFile: File): File {

        var exist = false
        var i = 0
        while (!exist) {
            var newFile = getNewFile(mAppName!!.trim(), i)
            Log.e(mTag, "$i Hi file name is ${newFile.name}")
            if (newFile.exists()) {
                Log.e(mTag, "$i Yes file exist ")
                i++
            } else {
                var nextFile = getNewFile(mAppName!!.trim(), i++)
                Log.e(mTag, "Next file is ${nextFile.name}")
                if (nextFile.exists()) {
                    i++
                    Log.e(mTag, "$i Current I is $i  ")
                } else {
                    exist = true
                    i--
                    Log.e(mTag, "$i Hi file name is Not found ")
                }
            }
        }

        //
        Log.e(mTag, "Z last  is $i")

        /** So if the file exist then get the name  */
        var name = apkFile.name // CIT_0.apk
        var size = name.length  //CIT_0.apk = 9

        var removeDotAndAPKfromName = name.substring(0, size - 4)   // CIT_0

        var number = ""
        var text = ""
        var isReach = false
        for (i in removeDotAndAPKfromName.indices) {
            if (isReach) {
                number = number.plus(removeDotAndAPKfromName[i])
            } else {
                if (removeDotAndAPKfromName[i].toString() == "_") {
                    isReach = true
                } else {
                    text = text.plus(removeDotAndAPKfromName[i])
                }
            }
        }

        Log.e(mTag, "Z i is $i")
        Log.e(mTag, "Z Text is $text")
        Log.e(mTag, "Z getLastFourObject $removeDotAndAPKfromName")
        Log.e(mTag, "Z Old name is $name ${name.length}")

        return File(mDire, "${text}_${i}${extension}")
    }

    private fun getNewFile(appName: String, i: Int): File =
        File(mDire, "${appName.trim()}_${i}${extension}")

    /*** check used to set download listener interface */
    private fun getListener() = mOnItemResultListener

    fun setOnDownloadListener(mOnDownloadListener: OnDownloadListener) {
        this.mOnItemResultListener = mOnDownloadListener
    }

    interface OnDownloadListener {
        fun onFileName(fileName: String)
        fun onStart()
        fun onFinish()
        fun onError(message: String)
        fun onNoInternetConnect(status: Boolean, message: String)
        fun onDownloadSuccess(status: Boolean)
        fun onDownloadFiled(message: String?, status: Boolean)
        fun onPermissionResult(permission: Boolean, message: String)
    }

    /**
     *
     *
     *
     *
     *
     */
    private fun writeResponseBody(body: ResponseBody): Boolean {
        return try {

            var isFolderCreated = false

            /** [1] - Check if the dire is found or not */
            if (!mDire!!.exists()) {
                if (mDire!!.mkdirs()) {
                    isFolderCreated = true
                }
            } else {
                Log.e(
                    mTag,
                    "folder (${mDire!!.absolutePath}) is already created $isFolderCreated - ${mDire!!.isHidden}"
                )
            }

            /** Main file name */
            val apkFile = File(mDire, "${mAppName}_0${extension}")

            /** destinationFile file */
            var destinationFile: File? = null

            destinationFile = try {

                if (isFolderCreated) {
                    apkFile
                } else {
                    if (apkFile.exists()) {
                        Log.e(mTag, "file size is ${apkFile.length()}")
                        getNewFileName(apkFile)
                    } else {
                        Log.e(mTag, "First file is " + apkFile.name)
                        apkFile
                    }
                }
            } catch (e: Exception) {
                Log.e(mTag, "Exception occur ${e.message}")
                apkFile
            }

            Log.e(mTag, "m 2 is ${apkFile.name}  -  ${destinationFile?.name}")


//            val futureStudioIconFile = File(dire, "${originalFileName+SystemClock.currentThreadTimeMillis()}_0${extension}")
            val futureStudioIconFile = apkFile
//            val futureStudioIconFile = File(dire, "${originalFileName}_0${extension}")

            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                val fileSize = body.contentLength()
                var fileSizeDownloaded: Long = 0
                inputStream = body.byteStream()
                outputStream = FileOutputStream(futureStudioIconFile)
                while (true) {
                    val read = inputStream.read(fileReader)
                    if (read == -1) {
                        break
                    }
                    outputStream.write(fileReader, 0, read)
                    fileSizeDownloaded += read.toLong()
                }
                Log.e(
                    mTag,
                    "file download: $fileSizeDownloaded of $fileSize - ${fileSizeDownloaded / 1024} MB"
                )
                outputStream.flush()
                true
            } catch (e: IOException) {
                Log.e(mTag, "IOException occur 1 -> $e")
                false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            Log.e(mTag, "IOException occur 2 -> $e")
            false
        }
    }
}
