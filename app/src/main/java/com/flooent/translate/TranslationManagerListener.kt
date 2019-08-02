package com.flooent.translate

interface TranslationManagerListener {
    fun onTranslationSuccessful(
        nativeInteraction: Boolean,
        translatedText: String,
        originalText: String
    )
    fun onTranslationFailure(message: String?)
    fun onLanguageDownloadSuccessful()
    fun onLanguageDownloadFailure()

}
