package com.flooent.translate

import android.util.Log
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions

class TranslationManager private constructor(
    private var view: TranslationManagerListener?
) {

    private var nativeToForeignTranslator: FirebaseTranslator? = null
    private var foreignToNativeTranslator: FirebaseTranslator? = null
    private var nativeLangCode: String = "en"
    private var foreignLangCode: String = "en"
    private var originalText=""

    companion object {
        private var instance: TranslationManager? = null

        fun getInstance(view: TranslationManagerListener): TranslationManager? {
            if (instance == null)
                instance = TranslationManager(view)

            return instance
        }
    }

    fun initTranslator(nativeLang: String, foreignLang: String) {

        nativeLangCode = nativeLang
        foreignLangCode = foreignLang

        changeNativeLang(nativeLang)

        changeForeignLang(foreignLang)
    }

    fun changeNativeLang(langCode: String) {
        nativeLangCode = langCode

        initNativeTranslator()
        verifyNativeModule()

        initForeignTranslator()
        verifyForeignModule()
    }

    fun changeForeignLang(langCode: String) {
        foreignLangCode = langCode

        initForeignTranslator()
        verifyForeignModule()

        initNativeTranslator()
        verifyNativeModule()
    }

    private fun verifyForeignModule() {
        foreignToNativeTranslator?.downloadModelIfNeeded()
            ?.addOnSuccessListener {
                // Model downloaded successfully. Okay to start translating.
                // (Set a flag, unhide the translation UI, etc.)
                Log.println(Log.ASSERT, "in verify model", "successful")
            }
            ?.addOnFailureListener { exception ->
                // Model couldn’t be downloaded or other internal error.
                // ...
                Log.println(Log.ASSERT, "in verify model", "failed = ${exception.message}")
            }

    }

    private fun verifyNativeModule() {
        nativeToForeignTranslator?.downloadModelIfNeeded()
            ?.addOnSuccessListener {
                // Model downloaded successfully. Okay to start translating.
                // (Set a flag, unhide the translation UI, etc.)
                view?.onLanguageDownloadSuccessful()
                Log.println(Log.ASSERT, "in verify model", "successful")
            }
            ?.addOnFailureListener { exception ->
                // Model couldn’t be downloaded or other internal error.
                // ...
                view?.onLanguageDownloadFailure()
                Log.println(Log.ASSERT, "in verify model", "failed")

            }
            ?.addOnCanceledListener {
                view?.onLanguageDownloadFailure()
                Log.println(Log.ASSERT, "in verify model", "canceled")
            }
    }

    private fun initNativeTranslator() {

        Log.e("native ", "$nativeLangCode")
        Log.e("foreign ", "$foreignLangCode")

        val options = FirebaseTranslatorOptions.Builder()
            .setSourceLanguage(FirebaseTranslateLanguage.languageForLanguageCode(nativeLangCode.toUpperCase())!!)
            .setTargetLanguage(FirebaseTranslateLanguage.languageForLanguageCode(foreignLangCode)!!)
            .build()

        nativeToForeignTranslator = FirebaseNaturalLanguage.getInstance().getTranslator(options)

    }

    private fun initForeignTranslator() {

        val options = FirebaseTranslatorOptions.Builder()
            .setSourceLanguage(FirebaseTranslateLanguage.languageForLanguageCode(foreignLangCode)!!)
            .setTargetLanguage(FirebaseTranslateLanguage.languageForLanguageCode(nativeLangCode)!!)
            .build()

        foreignToNativeTranslator = FirebaseNaturalLanguage.getInstance().getTranslator(options)

    }

    fun translateText(nativeInteraction: Boolean, resultText: String?) {

        originalText = resultText!!

        if (nativeInteraction)
            nativeToForeignTranslator?.translate(originalText)
                ?.addOnSuccessListener { translatedText ->
                    Log.e("inside native Trans", "")

                    view?.onTranslationSuccessful(nativeInteraction, translatedText, originalText)

                }
                ?.addOnFailureListener { exception ->
                    // Error.
                    // ...

                    view?.onTranslationFailure(exception.message)
                }
        else
            foreignToNativeTranslator?.translate(originalText)
                ?.addOnSuccessListener { translatedText ->
                    Log.e("inside foreign Trans", "")
                    view?.onTranslationSuccessful(nativeInteraction, translatedText, originalText)
                }
                ?.addOnFailureListener { exception ->
                    // Error.
                    // ...
                    view?.onTranslationFailure(exception.message)
                }
    }


    fun removeView() {
        view = null
    }

}