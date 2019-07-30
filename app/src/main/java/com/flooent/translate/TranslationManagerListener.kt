package com.flooent.translate

interface TranslationManagerListener {
    fun onTranslationSuccessful(nativeInteraction: Boolean, translatedText: String?)
    fun onTranslationFailure(message: String?)

}
