package com.jrumpe.githubsearch.extensions

import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout

fun EditText.isNotEmpty(textInputLayout: TextInputLayout): Boolean {
    return if (text.toString().isEmpty()) {
        textInputLayout.error = "Cannot be Blank!"
        false
    } else {
        textInputLayout.isErrorEnabled = false
        true
    }
}

