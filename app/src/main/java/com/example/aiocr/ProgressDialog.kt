package com.example.aiocr

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView

class ProgressDialog(context: Context): AlertDialog(context) {
    private val messageTextView: TextView

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
        messageTextView = view.findViewById(R.id.message)
        setView(view)
    }

    override fun setMessage(message: CharSequence?) {
        this.messageTextView.text = message.toString()
    }
}