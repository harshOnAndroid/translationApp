package com.flooent.translate

interface SpeechManagerListener {
    fun onReadyForSpeech()
    fun onError()
    fun onResults(isNativeInteraction: Boolean, resultText: String?)


}
