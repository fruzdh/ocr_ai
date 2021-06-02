package com.example.aiocr

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aiocr.databinding.ActivityMainBinding
import java.io.File
import java.io.InputStream


class MainActivity : AppCompatActivity(), OcrProcessHelper.OcrResultCallback {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding

    private var currentPhotoUri: Uri? = null
    private var photoFile: File? = null
    private var ocrProcessHelper: OcrProcessHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        ocrProcessHelper = OcrProcessHelper(this, this)

        binding?.fab?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                binding?.progressBar?.visibility = View.VISIBLE
                ocrProcessHelper?.prepareTesseract(this)
                pickImages.launch("image/*")
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )) {
                    Toast.makeText(this, "Need Permission", Toast.LENGTH_SHORT).show()
                } else {
                    requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) -> {
                    binding?.progressBar?.visibility = View.VISIBLE
                    ocrProcessHelper?.prepareTesseract(this)
                    pickImages.launch("image/*")
                }
                else -> {
                    requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            binding?.progressBar?.visibility = View.VISIBLE
            ocrProcessHelper?.prepareTesseract(this)
            pickImages.launch("image/*")
        } else {
            Toast.makeText(this, "Need Permission", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) {
            currentPhotoUri = it


            val `is`: InputStream? = contentResolver.openInputStream(currentPhotoUri!!)
            val options = BitmapFactory.Options()
            options.inSampleSize = 4
            val bmp: Bitmap? = BitmapFactory.decodeStream(`is`, null, options)

            ocrProcessHelper!!.processBitmapImage(this, bmp!!)
        }
    }

    override fun onOcrResult(text: String?) {
        binding?.progressBar?.visibility = View.INVISIBLE
        binding?.tv?.text = text
    }
}