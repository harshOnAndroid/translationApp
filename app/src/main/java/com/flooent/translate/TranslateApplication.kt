package com.flooent.translate

import android.app.Application
import com.google.firebase.FirebaseApp

class TranslateApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}