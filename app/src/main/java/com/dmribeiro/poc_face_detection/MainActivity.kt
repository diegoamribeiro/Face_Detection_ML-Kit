package com.dmribeiro.poc_face_detection

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.extensions.internal.ImageCaptureConfigProvider
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.dmribeiro.poc_face_detection.config.FaceDetectAnalyzer
import com.dmribeiro.poc_face_detection.databinding.ActivityMainBinding
import com.dmribeiro.poc_face_detection.utils.Constants
import com.dmribeiro.poc_face_detection.utils.extensions.observeOnce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), FaceDetectAnalyzer.SampleInterface {

    private val imageAnalyzer = FaceDetectAnalyzer(this, this)

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private var imageAnalysis: ImageAnalysis? = null

    private var isSmiling: MutableLiveData<Boolean> = MutableLiveData(false)
    private var rightEyeClosed: MutableLiveData<Boolean> = MutableLiveData(false)
    private var leftEyeClosed: MutableLiveData<Boolean> = MutableLiveData(false)
    private var shouldTakePhoto: MutableLiveData<Boolean> = MutableLiveData(false)
    private var definitive: LiveData<Boolean> = shouldTakePhoto

    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        outputDirectory = outputDirectory()



        if (allPermissionGranted()) {
            startCamera()
            Toast.makeText(this, "We Have Permission", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                this, Constants.REQUIRED_PERMISSIONS, Constants.REQUEST_CODE_PERMISSIONS
            )
        }



    }


    private fun checkParamsToTakePhoto(): MutableLiveData<Boolean> {
        return if (shouldTakePhoto.value == false){
            MutableLiveData(
            isSmiling.value!!
                    && rightEyeClosed.value!!
                    && leftEyeClosed.value!!
        )}else{
            return MutableLiveData(false)
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            definitive.observe(this@MainActivity, androidx.lifecycle.Observer {
                if(it){
                    binding.buttonTakePhoto.performClick()
                    Log.d("***click", it.toString())
                }
                //binding.buttonTakePhoto.visibility = View.GONE
            })
        }

        binding.buttonTakePhoto.setOnClickListener {
            takePhoto()
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val cameraPreviewFuture = ProcessCameraProvider.getInstance(this)
        cameraPreviewFuture.addListener(
            {
                val cameraProvider: ProcessCameraProvider = cameraPreviewFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also { itPreview ->
                        itPreview.setSurfaceProvider(binding.previewView.surfaceProvider)
                    }

                imageCapture =
                    ImageCapture.Builder().setJpegQuality(50).setTargetResolution(Size(360, 540))
                        .build()
                imageAnalysis = ImageAnalysis.Builder().build()
                imageAnalysis!!.setAnalyzer(
                    ContextCompat.getMainExecutor(this),
                    imageAnalyzer
                )

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(flipCamera())
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.d(Constants.TAG, "startCamera Fail", e)
                }
            }, ContextCompat.getMainExecutor(this)
        )
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                Constants.FILE_NAME_FORMAT,
                Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        CoroutineScope(Dispatchers.IO).launch {
            imageCapture.takePicture(outputOption, ContextCompat.getMainExecutor(this@MainActivity),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        val msg = "Photo saved"
                        Toast.makeText(this@MainActivity, "$msg + $savedUri", Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to save image",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            )
        }

    }

    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun getResult(values: List<Float>) {
        if (values[0] > 0.5f) {
            binding.tvSmile.text = "Sorrindo"
            isSmiling.value = true
        } else {
            binding.tvSmile.text = "Sério"
        }

        if (values[1] > 0.5f) {
            binding.tvRightEye.text = "Olho direito aberto"
            rightEyeClosed.value = true
        } else {
            binding.tvRightEye.text = "Olho direito fechado"
        }

        if (values[2] > 0.5f) {
            binding.tvLeftEye.text = "Olho esquerdo aberto"
            leftEyeClosed.value = true
        } else {
            binding.tvLeftEye.text = "Olho esquerdo fechado"
        }

        if (isSmiling.value == true && rightEyeClosed.value == true && leftEyeClosed.value == true && definitive.value == false){
            shouldTakePhoto.value = true
        }

//        if (checkParamsToTakePhoto().value!!) {
//            shouldTakePhoto.value = true
//        }
    }

    private fun outputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let { file ->
            File(file, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else {
            filesDir
        }
    }

    private fun flipCamera(): Int {
        binding.btnChange.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            startCamera()
        }
        return lensFacing
    }
}