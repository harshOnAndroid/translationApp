package com.flooent.translate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechManager private constructor(
    private var view: TranslationScreenView,
    private var applicationContext: Context
) : RecognitionListener {


    private lateinit var speechRecognizer: SpeechRecognizer
    private var isNativeInteraction = true
    private var isNativeLangSelection = true
    private var nativeLangCode: String = "us-en"
    private var foreignLangCode: String = "us-en"

    init {
        initSpeechRecognition()
    }

    companion object{
        private var instance: SpeechManager? = null

        fun getInstance(view:TranslationScreenView, context:Context) : SpeechManager? {

            if(instance == null)
                instance = SpeechManager(view,context)

            return instance
        }

    }

    private fun initSpeechRecognition() {

        if (SpeechRecognizer.isRecognitionAvailable(applicationContext)) {

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)

            speechRecognizer.setRecognitionListener(this)

        } else {
            Log.e("speech recognition", "not available")
        }


    }

    fun startListeningToSpeech(nativeInteraction: Boolean) {
        isNativeInteraction = nativeInteraction

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        if (isNativeInteraction)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, nativeLangCode)
        else
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, foreignLangCode)

        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            applicationContext.getString(R.string.speech_prompt)
        )

        speechRecognizer.startListening(intent)

        /*val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, application.packageName)
        speechRecognizer.startListening(intent)*/
    }


    override fun onReadyForSpeech(p0: Bundle?) {
        Log.e("speech recognition", "onReadyForSpeech")
        view.onReadyForSpeech()
    }

    override fun onRmsChanged(p0: Float) {
        Log.e("speech recognition", "onRmsChanged")
    }

    override fun onBufferReceived(p0: ByteArray?) {
        Log.e("speech recognition", "onBufferReceived")
    }

    override fun onPartialResults(p0: Bundle?) {
        Log.e("speech recognition", "onPartialResults")
    }

    override fun onEvent(p0: Int, p1: Bundle?) {
        Log.e("speech recognition", "onEvent")
    }

    override fun onBeginningOfSpeech() {
        Log.e("speech recognition", "onBeginningOfSpeech")
    }

    override fun onEndOfSpeech() {
        Log.e("speech recognition", "onEndOfSpeech")
    }

    override fun onError(p0: Int) {
        Log.e("speech recognition", "onError= $p0")
        view.onError()
    }

    override fun onResults(p0: Bundle?) {

        val results = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

        val scores = ArrayList<String>()

        for (i in p0?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)!!) {
            scores.add(i.toString())
        }

        Log.e(
            "speech recognition", "onResults = $results " +
                    "confidence vals = $scores"
        )

        view.onResults(isNativeInteraction, results?.get(0))

    }
}