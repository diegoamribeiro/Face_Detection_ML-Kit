package com.dmribeiro.poc_face_detection.config

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectAnalyzer(val context: Context, val si: SampleInterface) : ImageAnalysis.Analyzer {

    val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {

        imageProxy.image?.let {
            val inputImage = InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees)

            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    faces.forEach { face ->

                        si.getResult(
                            listOf(
                                face.smilingProbability,
                                face.rightEyeOpenProbability,
                                face.leftEyeOpenProbability
                            ) as List<Float>
                        )

//                        si.getResult(
//                            " Faces = ${faces.size}\n " +
//                                    "Abertura de sorriso = " + "${"%.2f".format(face.smilingProbability)}\n" +
//                                    " Abertura do olho esquerdo = ${"%.2f".format(face.leftEyeOpenProbability)}\n" +
//                                    " Abertura do olho direito = ${"%.2f".format(face.rightEyeOpenProbability)}"
//                        )

//                        Log.d("***Face", face.smilingProbability.toString())
//                        Log.d("***Face", face.leftEyeOpenProbability.toString())
//                        Log.d("***Face", face.rightEyeOpenProbability.toString())
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("Face", exception.toString())
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
            ?: imageProxy.close()
    }

    fun convertValues(){

    }

    interface SampleInterface {
        fun getResult(values: List<Float>)
    }
}