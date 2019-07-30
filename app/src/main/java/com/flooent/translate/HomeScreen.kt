package com.flooent.translate

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_home_screen.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset

import java.util.*
import kotlin.collections.ArrayList

class HomeScreen : AppCompatActivity(), SpeechManagerListener, TranslationManagerListener {

    private var translationManager: TranslationManager? = null
    private var nativeToForeignTranslator: FirebaseTranslator? = null
    private var foreignToNativeTranslator: FirebaseTranslator? = null
    private var speechManager: SpeechManager? = null
    private var nativeLangCode: String = "en"
    private var foreignLangCode: String = "en"
    private var allLangs: ArrayList<Langs>? = null
    private lateinit var speechRecognizer: SpeechRecognizer
    private val REQ_CODE_SPEECH_INPUT = 100
    private var isNativeInteraction = true
    private var isNativeLangSelection = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        speechManager = SpeechManager.getInstance(this, applicationContext)
        translationManager = TranslationManager.getInstance(this)

        img_speakNative.setOnClickListener {
            //promptSpeechInput()
            isNativeInteraction = true
            listenAndTranslate(true)
        }

        img_flagNative.setOnClickListener {
            isNativeLangSelection = true
            showLangsDialog()
        }

        img_speakForeign.setOnClickListener {
            //promptSpeechInput()
            isNativeInteraction = false
            listenAndTranslate(false)

        }

        img_flagForeign.setOnClickListener {
            isNativeLangSelection = false
            showLangsDialog()
        }

        translationManager?.initTranslator(nativeLangCode, foreignLangCode)

    }

    private fun listenAndTranslate(isNativeInteraction: Boolean) {

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            speechManager?.startListeningToSpeech(isNativeInteraction)
            showSpeakNowToast()
        } else {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

    }

    private fun showSpeakNowToast() {

        Toast.makeText(this,"Speak Now",Toast.LENGTH_SHORT).show()

//        val toast = Toast(this)
//        toast.duration = Toast.LENGTH_SHORT
//        toast.setMargin(10f,200f)
//        toast.setText("Speak Now")
//        toast.show()
    }


    override fun onDestroy() {
        super.onDestroy()

        speechManager?.removeView()
        translationManager?.removeView()
    }


    /**
     * Showing google speech input dialog
     */
    private fun promptSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            getString(R.string.speech_prompt)
        )
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (a: ActivityNotFoundException) {
            Toast.makeText(
                getApplicationContext(),
                getString(R.string.speech_not_supported),
                Toast.LENGTH_SHORT
            ).show()
        }

    }


    fun showLangsDialog() {
        val langsStr = java.util.ArrayList<String>()

        val langs = getLangsArrayList()
        for (i in langs?.indices!!) {
            val lan1 = langs.get(i)
            langsStr.add(lan1.name)
        }
        val langStrArr = langsStr.toTypedArray()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select a Language")
        builder.setItems(langStrArr,
            DialogInterface.OnClickListener { dialog, which -> setSelectedLang(which) })
        builder.show()
    }

    private fun setSelectedLang(which: Int) {
        val selectedLang = allLangs?.get(which)

        if (isNativeLangSelection) {
            txt_langNative.text = selectedLang?.name
            nativeLangCode = selectedLang?.code!!
            speechManager?.changeNativeLang(nativeLangCode)
            translationManager?.changeNativeLang(nativeLangCode)
        } else {
            txt_langForeign.text = selectedLang?.name
            foreignLangCode = selectedLang?.code!!
            speechManager?.changeForeignLang(foreignLangCode)
            translationManager?.changeForeignLang(foreignLangCode)
        }
    }

    fun getLangsArrayList(): java.util.ArrayList<Langs>? {
        try {
            val jsonObject = JSONObject(loadLanguageJSONFromAsset())
            allLangs = Gson().fromJson<ArrayList<Langs>>(
                jsonObject.getString("langs"),
                object : TypeToken<ArrayList<Langs>>() {

                }.type
            )

            return allLangs
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return null
    }

    fun loadLanguageJSONFromAsset(): String? {
        var json: String?
        try {
            val asset = applicationContext.assets.open("supported_langs2.json")
            val size = asset.available()
            val buffer = ByteArray(size)
            asset.read(buffer)
            asset.close()
            json = buffer.toString(Charset.forName("UTF-8"))
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }

        Log.e("lang json ", "\n $json")
        return json
    }


    /**
     * Receiving speech input
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_CODE_SPEECH_INPUT -> {
                if (resultCode == Activity.RESULT_OK && null != data) {

                    val result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    txt_speechOutputNative.setText(result.get(0))
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            speechManager?.startListeningToSpeech(isNativeInteraction)
        } else
            Toast.makeText(applicationContext, "Cannot proceed without audio permission", Toast.LENGTH_LONG).show()
    }


    override fun onReadyForSpeech() {
        layout_progress.visibility = View.VISIBLE
//        Toast.makeText()
    }

    override fun onError() {
        layout_progress.visibility = View.GONE
    }


    override fun onResults(isNativeInteraction: Boolean, resultText: String?) {
        if (isNativeInteraction)
            txt_speechOutputNative.text = resultText
        else
            txt_speechOutputForeign.text = resultText


        layout_progress.visibility = View.GONE

        translationManager?.translateText(isNativeInteraction, resultText)
    }

    override fun onTranslationSuccessful(nativeInteraction: Boolean, translatedText: String?) {
        if (nativeInteraction)
            txt_speechOutputForeign.text = translatedText
        else
            txt_speechOutputNative.text = translatedText
    }

    override fun onTranslationFailure(message: String?) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }


    /*override fun onReadyForSpeech(p0: Bundle?) {
        Log.e("speech recognition", "onReadyForSpeech")
        layout_progress.visibility = View.VISIBLE
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
        layout_progress.visibility = View.GONE
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

        if (isNativeInteraction)
            txt_speechOutputNative.setText(results?.get(0))
        else
            txt_speechOutputForeign.text = results?.get(0)


        layout_progress.visibility = View.GONE

    }*/
}

