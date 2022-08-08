package com.dmribeiro.poc_face_detection.utils.extensions

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.io.IOException


@Throws(IOException::class)
private fun Uri.getBitmapFromUri(activity: Activity): Bitmap? {
    val parcelFileDescriptor: ParcelFileDescriptor? =
        activity.contentResolver.openFileDescriptor(this, "r")
    val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
    val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
    parcelFileDescriptor.close()
    return image
}

fun <T> MutableLiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>){
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T) {
            observer.onChanged(t)
            if (t != null){
                removeObserver(this)
            }
        }
    })
}

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>){
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T) {
            observer.onChanged(t)
            if (t != null){
                removeObserver(this)
            }
        }
    })
}