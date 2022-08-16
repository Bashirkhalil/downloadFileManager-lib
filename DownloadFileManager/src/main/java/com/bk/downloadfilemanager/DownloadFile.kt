package com.bk.downloadfilemanager

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
import retrofit2.Retrofit
import java.io.*

class DownloadFile {

    private var mTag = DownloadFile::class.java.simpleName
    private var mURL: String? = null
    private var appName: String = "defaultName"
    private lateinit var mFile: File

    private var mFolder: String? = null
    private var mBaseURL: String? = null
    private var mOnItemResultListener: OnDownloadListener? = null

    lateinit var mContext: Context
    private var mJob: Job? = null
    private val coroutineExceptionHandler = CoroutineExceptionHandler { context, throwable ->
        Log.e(
            mTag,
            "coroutineExceptionHandler -> ${throwable.message} - ${throwable.printStackTrace()}"
        )
        getListener()?.onDownloadFiled(throwable.message, false)
    }

    fun setUrl(url: String) {
        this.mURL = url
    }

    fun setFile(mFile: File) {
        this.mFile = mFile
    }

    fun setBaseURl(BaseURL: String) {
        mBaseURL = BaseURL
    }

    fun setFolder(yourFolderName: String) {
        mFolder = yourFolderName
    }

    fun setFileName(fileName: String) {
        appName = fileName
    }

    fun build(context: Context) {
        mContext = context


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                mContext.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                getListener()?.onPermissionResult(false,"Plz Grand permission in order to download file !!!")
                return
            }
        }


        if (!isConnect(mContext)) {
            mOnItemResultListener?.onNoInternetConnect(false,"Plz check Internet connection !!!")
            return
        }

        if (mBaseURL.isNullOrEmpty()) {
            mOnItemResultListener?.onError("Please enter the BaseUrl Eg : https://apkfab.com/upload/apk/ ")
            return
        }

        if (mBaseURL?.contains("apk") != true) {
            mOnItemResultListener?.onError("Please enter the BaseUrl Eg : https://apkfab.com/upload/apk/ ")
            return
        }

        if (mURL.isNullOrEmpty()) {
            mOnItemResultListener?.onError("Please enter the mURL Eg : courses/archive/spr11/cos217/lectures/08DsAlg.pdf ")
            return
        }

        if (mFile == null) {
            mOnItemResultListener?.onError("File is required Eg : var targetFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS}/AHT/, a.apk) ")
        }

        executeDownloading()

    }

    private fun executeDownloading() {

        getListener()?.onStart()

        mJob = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            val httpClient = OkHttpClient.Builder()
            val builder = Retrofit.Builder().baseUrl(mBaseURL)
            val retrofit = builder.client(httpClient.build()).build()
            var mApiServices = retrofit.create(ApiServices::class.java)
            var responseResult = mApiServices.downloadFileByUrl(mURL)

            if (responseResult.isSuccessful) {
//                responseResult.body()?.let {

//                    val task = object : AsyncTask<Void, Void, Void>() {
//                        override fun doInBackground(vararg voids: Void): Void? {

                responseResult.body()?.let {
                    val writtenToDisk = writeResponseBodyToDiskAndDeployment(it, appName)
                    if (writtenToDisk) {
                        withContext(Dispatchers.Main) {
                            getListener()?.onFinish()
                            getListener()?.onDownloadSuccess(true)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            getListener()?.onDownloadFiled(
                                responseResult.message(),
                                false
                            )
                        }
                    }
                }
//                            return null
//                        }
//                    }
//                    task.execute()

//                }
            } else {
                Log.e(
                    mTag,
                    "Response Err Error -> ${responseResult.message()} -  ${responseResult.code()}"
                )
                withContext(Dispatchers.Main) {
                    getListener()?.onError("${responseResult.message()} - ${responseResult.code()}")
                }
            }


            /*
            if (responseResult.isSuccessful) {
                responseResult.body()?.let {
                    val writtenToDisk = writeResponseBodyToDiskFFFFFFFFF(it, appName)
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
                Log.e(
                    mTag,
                    "Response Err Error -> ${responseResult.message()} -  ${responseResult.code()}"
                )
                getListener()?.onError("${responseResult.message()} - ${responseResult.code()}")
            }
             */
        }

    }

    private fun getListener() = mOnItemResultListener

    fun setOnDownloadListener(mOnDownloadListener: OnDownloadListener) {
        this.mOnItemResultListener = mOnDownloadListener
    }

    interface OnDownloadListener {
        fun onFileName(fileName: String)
        fun onStart()
        fun onFinish()
        fun onError(message: String)
        fun onNoInternetConnect(status: Boolean,message: String)
        fun onDownloadSuccess(status: Boolean)
        fun onDownloadFiled(message: String?, status: Boolean)
        fun onPermissionResult(permission: Boolean, message: String)
    }


    private fun writeResponseBodyToDiskAndDeployment(body: ResponseBody, appName: String): Boolean {

        var isFolderCreated = false

        /** [1] - Check if the dire is found or not */
        var dire =
            File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/AHT/")
        if (!dire.exists()) {
            if (dire.mkdirs()) {
                isFolderCreated = true
            }
        } else {
            Log.e(mTag, "folder is already created $isFolderCreated")
        }

//        for (i in 0..1000 ) {
//            Log.e(mTag, "folder is created $isFolderCreated - ${Helper.getTimeInmill()}")
//        }

        /** Main file name */
        val apkFile = File(
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/AHT/",
            "${appName}_0.apk"
        )

        /** destinationFile file */
        var destinationFile: File? = null
        if (isFolderCreated) {
            destinationFile = apkFile  // Here  mean it's first time he come here
        } else {


            if (apkFile.exists()) {

                var exist = false
                var i = 0
                while (!exist) {
                    var newFile = getNewFile(appName.trim(), i)
                    Log.e(mTag, "$i Hi file name is ${newFile.name}")
                    if (newFile.exists()) {
                        Log.e(mTag, "$i Yes file exist ")
                        i++
                    } else {
                        var nextFile = getNewFile(appName.trim(), i++)
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

                destinationFile = File(
                    "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/AHT/",
                    "${text}_${i}.apk"
                )

            } else {
                /** Destination file will save in AHT */
                Log.e(mTag, "First file is " + apkFile.name)
                destinationFile = apkFile
            }

        }


        /*********************************/
        // ********n Deployment File here
        try {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(destinationFile)
//                var m = copyStream(inputStream, outputStream)
                var m = copyStream2(inputStream, outputStream)

                return m

            } catch (e: Exception) {
                getListener()?.onError("1 Exception occur -> $e")
                return false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: Exception) {
            getListener()?.onError("2 Exception occur -> $e")
            return false
        }

    }

    @Throws(IOException::class)
    fun copyStream(input: InputStream, output: OutputStream): Boolean {
        return try {
            val buffer = ByteArray(1024) // Adjust if you want
            var bytesRead: Int
            while (input.read(buffer).also {
                    bytesRead = it
                } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            true
        } catch (e: Exception) {
            Log.e(mTag, "Exception occur  $e")
            false
        }

    }

    @Throws(IOException::class)
    fun copyStream2(input: InputStream, output: OutputStream): Boolean {
        return try {
            val fileReader = ByteArray(4096)
            while (true) {
                val read = input!!.read(fileReader)
                if (read == -1) {
                    break
                }
                output.write(fileReader, 0, read)
                output.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(mTag, "Exception occur  $e")

            false
        }

    }

    private fun getNewFile(appName: String, i: Int): File {
        return File(
            Environment.getExternalStorageDirectory()
                .toString() + "/${Environment.DIRECTORY_DOWNLOADS}/AHT/${appName.trim()}_${i}.apk"
        )
    }

    private fun isConnect(mContext: Context): Boolean {
        val connectivityManager =
            mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
        return false
    }

}

/*

//                    CoroutineScope(Dispatchers.IO).launch {
//                        var there = true
//                        var m=1
//
//                        while(there){
//                            if (destinationFile.exists()){
//                                Log.e("m","${m++} File is exist : ${Helper.getTime()}")
//                                break
//                            }else{
//                                Log.e("m","${m++} File not exist : ${Helper.getTime()}")
//                            }
//
////                            delay(500)
//                        }
//                    }
//
 */

/*
  private fun writeFileToDesk(body: ResponseBody, appName: String): Boolean {
        return try {


            /** create file directory */
            mFile?.let {
                /** if the folder not exist then create */
                if (!it.exists()) it.mkdir()
            }

//            Log.e(mTag, "jkkss ${mFile?.isDirectory}")
//            Log.e(mTag, "jkkss ${mFile?.absolutePath}")
//            Log.e(mTag, "jkkss ${mFile?.path}")
//            Log.e(mTag, "jkkss ${mFile?.absoluteFile}")
//

            /** check if file exist */
            var destinationFile: File? = null


            var apkFile = File(
                Environment.getExternalStorageDirectory()
                    .toString() + "/${Environment.DIRECTORY_DOWNLOADS}/AHT/${appName.trim()}_0.apk"
            )

            if (apkFile.exists()) {

                var exist = false
                var i = 0
                while (!exist) {
                    var newFile = getNewFile(appName.trim(), i)
                    Log.e(mTag, "$i Hi file name is ${newFile.name}")
                    if (newFile.exists()) {
                        Log.e(mTag, "$i Yes file exist ")
                        i++
                    } else {
                        var nextFile = getNewFile(appName.trim(), i++)
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
//                Log.e(mTag, "Z increase is ${increaseNumber(i)}")
                Log.e(mTag, "Z Text is $text")
                Log.e(mTag, "Z getLastFourObject $removeDotAndAPKfromName")
                Log.e(mTag, "Z Old name is $name ${name.length}")

                var newNameIs = "${text}_$i" //${increaseNumber(i)}"
                destinationFile = File(
                    Environment.getExternalStorageDirectory()
                        .toString() + "/${Environment.DIRECTORY_DOWNLOADS}/AHT/${newNameIs.trim()}.apk"
                )

            } else {
                /** Destination file will save in AHT */
                Log.e(mTag, "First file is " + apkFile.name)
                destinationFile = apkFile
            }

//
            Log.e(mTag, "Output ${destinationFile.absolutePath}")

            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {

                val fileReader = ByteArray(4096)
                val fileSize = body.contentLength()
                var fileSizeDownloaded: Long = 0

                inputStream = body.byteStream()
                outputStream = FileOutputStream(destinationFile)
                Log.e(
                    mTag,
                    "file download: fileSizeDownloaded : $fileSizeDownloaded of body.contentLength() : $fileSize"
                )

                return copyStream(inputStream, outputStream)

            } catch (e: IOException) {
                Log.e(mTag, "1 IOException $e")

                false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            Log.e(mTag, "2 IOException $e")
            false
        }
    }

 */