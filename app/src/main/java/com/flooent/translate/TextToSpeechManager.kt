package com.flooent.translate

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.*

class TextToSpeechManager private constructor(context:Context):TextToSpeech.OnInitListener{

    private var ttsNative: TextToSpeech
    private var ttsForeign: TextToSpeech

    init {
        ttsNative = TextToSpeech(context, this)
        ttsForeign = TextToSpeech(context, this)

        setNativeLanguage("en")
        setForeignLanguage("en")
    }

    companion object{
        private var instance : TextToSpeechManager? = null

        fun getInstance(context: Context):TextToSpeechManager?{
            if(instance == null)
                instance = TextToSpeechManager(context)

            return instance
        }
    }

    override fun onInit(p0: Int) {

    }

    fun speakNative(text:String){
        if (android.os.Build.VERSION.SDK_INT>21)
            ttsNative.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        else
            ttsNative.speak(text, TextToSpeech.QUEUE_FLUSH, null)
    }

    fun speakForeign(text:String){
        if (android.os.Build.VERSION.SDK_INT>21)
            ttsForeign.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        else
            ttsForeign.speak(text, TextToSpeech.QUEUE_FLUSH, null)
    }

    fun setNativeLanguage(lang:String){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ttsNative.setLanguage(Locale.forLanguageTag(lang))
        }
    }


    fun setForeignLanguage(lang:String){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ttsForeign.setLanguage(Locale.forLanguageTag(lang))
        }
    }
}