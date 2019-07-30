package com.flooent.translate

interface TranslationScreenView {
    fun onReadyForSpeech()
    fun onError()
    fun onResults(isNativeInteraction: Boolean, resultText: String?)


}
