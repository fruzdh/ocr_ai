package com.example.aiocr

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.*

class OcrProcessHelper(
    appCompatActivity: AppCompatActivity,
    private val ocrResultCallback: OcrResultCallback
) {
    private var mTessOCR: TessBaseAPI? = null
    private var ocrText: String? = null
    private val dataPath: String = appCompatActivity.applicationContext.filesDir.path + "/TesseractSample/"

    companion object {
        private val TAG = OcrProcessHelper::class.java.simpleName
        private const val TESSDATA = "tessdata"
    }

    private fun copyTessDataFiles(path: String, appCompatActivity: AppCompatActivity) {
        val assetManager = appCompatActivity.assets
        var files: Array<String>? = null

        try {
            files = assetManager.list("")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (files == null) {
            return
        }

        for (filename in files) {
            var `in`: InputStream
            var out: OutputStream
            try {
                `in` = assetManager.open(filename)
                val outFile = File(path, filename)
                out = FileOutputStream(outFile)
                copyFile(`in`, out)
                `in`.close()
                out.flush()
                out.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int

        while (`in`.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }

    private fun prepareDirectory(path: String) {
        val dir = File(path)

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(
                    TAG, "ERROR: Creation of directory " + path +
                            " failed, check does Android Manifest have permission to write to external storage."
                )
            }
        } else {
            Log.i(TAG, "Created directory $path")
        }
    }

    fun prepareTesseract(appCompatActivity: AppCompatActivity) {

        try {
            prepareDirectory(dataPath + TESSDATA)
            copyTessDataFiles(dataPath + TESSDATA, appCompatActivity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun processBitmapImage(appCompatActivity: AppCompatActivity, bitmap: Bitmap) {

        Thread {
            val grayscaleImg = setGrayscale(bitmap)
            val noiceRemovedImage = removeNoise(grayscaleImg)
            ocrText = extractText(noiceRemovedImage)
            appCompatActivity.runOnUiThread {
                ocrResultCallback.onOcrResult(ocrText)
            }
        }.start()
    }

    private fun setGrayscale(img: Bitmap): Bitmap {
        val bmap = img.copy(img.config, true)
        var c: Int

        for (i in 0 until bmap.width) {
            for (j in 0 until bmap.height) {
                c = bmap.getPixel(i, j)
                val gray = (.299 * Color.red(c) + .587 * Color.green(c) + .114 * Color.blue(c)).toInt()
                bmap.setPixel(i, j, Color.argb(255, gray, gray, gray))
            }
        }

        return bmap
    }

    private fun removeNoise(bmap: Bitmap): Bitmap {
        for (x in 0 until bmap.width) {
            for (y in 0 until bmap.height) {
                val pixel = bmap.getPixel(x, y)
                if (Color.red(pixel) < 162 && Color.green(pixel) < 162 && Color.blue(pixel) < 162) {
                    bmap.setPixel(x, y, Color.BLACK)
                }
            }
        }

        for (x in 0 until bmap.width) {
            for (y in 0 until bmap.height) {
                val pixel = bmap.getPixel(x, y)
                if (Color.red(pixel) > 162 && Color.green(pixel) > 162 && Color.blue(pixel) > 162) {
                    bmap.setPixel(x, y, Color.WHITE)
                }
            }
        }

        return bmap
    }

    private fun extractText(bitmap: Bitmap): String {
        try {
            mTessOCR = TessBaseAPI()
            mTessOCR?.setDebug(true)
        } catch (e: Exception) {
            e.printStackTrace()
            if (mTessOCR == null) {
                Log.e(TAG, "TessBaseAPI is null. TessFactory not returning tess object.")
            }
        }

        mTessOCR?.init(dataPath, "eng")
        Log.d(TAG, "Training file loaded")
        mTessOCR?.setImage(bitmap)
        var extractedText = "Empty/Null Result"

        try {
            extractedText = mTessOCR?.utF8Text.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error in recognizing text.")
        }

        mTessOCR?.end()

        return extractedText
    }

    interface OcrResultCallback {
        fun onOcrResult(text: String?)
    }
}