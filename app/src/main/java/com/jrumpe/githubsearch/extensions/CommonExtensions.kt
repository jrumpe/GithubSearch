package com.jrumpe.githubsearch.extensions

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.gson.GsonBuilder
import com.jrumpe.githubsearch.models.ErrorResponse
import okhttp3.ResponseBody
import java.io.IOException

fun Context.showErrorMessage(errorBody: ResponseBody, duration: Int = Toast.LENGTH_SHORT) {
    val gson = GsonBuilder().create()
    try {
        val errorResponse = gson.fromJson(errorBody.toString(), ErrorResponse::class.java)
        toast(errorResponse.message!!, duration)
    } catch (e: IOException) {
        Log.i("Exception ", e.toString())
    }
}

/* Extension Functiom
* use Activity.toast -> access limited to activity class
* use context.toast unlimited access
* */
fun Context.toast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, msg, duration).show()
}

fun <T> List<T>.toArrayList(): ArrayList<T>{
    return ArrayList(this)
}